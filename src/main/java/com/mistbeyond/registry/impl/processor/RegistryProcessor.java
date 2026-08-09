package com.mistbeyond.registry.impl.processor;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Compile-time replacement for the runtime registration checkers.
 * <p>
 * Validates that every type carrying a registration annotation extends the required
 * Minecraft/NeoForge superclass and exposes exactly one correctly shaped static
 * registration method. Diagnostics are anchored to the offending source element,
 * so invalid mod code fails during compilation instead of at mod startup.
 */
@SupportedAnnotationTypes({
        "com.mistbeyond.registry.RegisterBlock",
        "com.mistbeyond.registry.RegisterItem",
        "com.mistbeyond.registry.RegisterMenuType",
        "com.mistbeyond.registry.RegisterContainerScreen",
        "com.mistbeyond.registry.RegisterBlockEntityType"
})
@SupportedSourceVersion(SourceVersion.RELEASE_25)
public final class RegistryProcessor extends AbstractProcessor {
    private static final String SUBSCRIBE_REGISTRATION = "com.mistbeyond.registry.SubscribeRegistration";
    private static final String PROVIDE_FACTORY = "com.mistbeyond.registry.ProvideFactory";

    private static final String BLOCK = "net.minecraft.world.level.block.Block";
    private static final String ITEM = "net.minecraft.world.item.Item";
    private static final String ABSTRACT_CONTAINER_MENU = "net.minecraft.world.inventory.AbstractContainerMenu";
    private static final String ABSTRACT_CONTAINER_SCREEN = "net.minecraft.client.gui.screens.inventory.AbstractContainerScreen";
    private static final String BLOCK_ENTITY = "net.minecraft.world.level.block.entity.BlockEntity";
    private static final String BLOCK_ENTITY_SUPPLIER = "net.minecraft.world.level.block.entity.BlockEntityType.BlockEntitySupplier";

    private static final String BLOCK_REGISTRATION = "com.mistbeyond.registry.impl.BlockRegistration";
    private static final String ITEM_REGISTRATION = "com.mistbeyond.registry.impl.ItemRegistration";
    private static final String MENU_TYPE_REGISTRATION = "com.mistbeyond.registry.impl.MenuTypeRegistration";
    private static final String CONTAINER_SCREEN_REGISTRATION = "com.mistbeyond.registry.impl.ContainerScreenRegistration";

    private sealed interface MethodContract permits SubscribeContract, FactoryContract {
    }

    private record SubscribeContract(String receiverType) implements MethodContract {
    }

    private record FactoryContract(String factoryType) implements MethodContract {
    }

    private record Contract(String requiredSuperclass, String methodAnnotation, MethodContract method) {
    }

    private static final Map<String, Contract> CONTRACTS = Map.of(
            "com.mistbeyond.registry.RegisterBlock",
            new Contract(BLOCK, SUBSCRIBE_REGISTRATION, new SubscribeContract(BLOCK_REGISTRATION)),
            "com.mistbeyond.registry.RegisterItem",
            new Contract(ITEM, SUBSCRIBE_REGISTRATION, new SubscribeContract(ITEM_REGISTRATION)),
            "com.mistbeyond.registry.RegisterMenuType",
            new Contract(ABSTRACT_CONTAINER_MENU, SUBSCRIBE_REGISTRATION, new SubscribeContract(MENU_TYPE_REGISTRATION)),
            "com.mistbeyond.registry.RegisterContainerScreen",
            new Contract(ABSTRACT_CONTAINER_SCREEN, SUBSCRIBE_REGISTRATION, new SubscribeContract(CONTAINER_SCREEN_REGISTRATION)),
            "com.mistbeyond.registry.RegisterBlockEntityType",
            new Contract(BLOCK_ENTITY, PROVIDE_FACTORY, new FactoryContract(BLOCK_ENTITY_SUPPLIER))
    );

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (TypeElement annotation : annotations) {
            Contract contract = CONTRACTS.get(annotation.getQualifiedName().toString());
            if (contract == null) {
                continue;
            }
            for (Element element : roundEnv.getElementsAnnotatedWith(annotation)) {
                if (element instanceof TypeElement type) {
                    checkType(type, contract);
                }
            }
        }
        return false;
    }

    private void checkType(TypeElement type, Contract contract) {
        checkSuperclass(type, contract.requiredSuperclass());
        checkRegistrationMethod(type, contract);
    }

    private void checkSuperclass(TypeElement type, String requiredSuperclassName) {
        TypeElement requiredSuperclass = processingEnv.getElementUtils().getTypeElement(requiredSuperclassName);
        if (requiredSuperclass == null) {
            error(type, String.format(
                    "registry-lib processor cannot resolve required superclass '%s'; make sure Minecraft/NeoForge is on the compile classpath.",
                    requiredSuperclassName
            ));
            return;
        }
        if (!processingEnv.getTypeUtils().isAssignable(type.asType(), processingEnv.getTypeUtils().erasure(requiredSuperclass.asType()))) {
            error(type, String.format(
                    "'%s' must extend or implement '%s'.",
                    type.getQualifiedName(),
                    requiredSuperclassName
            ));
        }
    }

    private void checkRegistrationMethod(TypeElement type, Contract contract) {
        var methods = ElementFilter.methodsIn(type.getEnclosedElements()).stream()
                .filter(method -> hasAnnotation(method, contract.methodAnnotation()))
                .filter(method -> method.getModifiers().contains(Modifier.STATIC))
                .toList();
        if (methods.isEmpty()) {
            error(type, String.format(
                    "No static method in '%s' is annotated with @%s.",
                    type.getQualifiedName(),
                    simpleName(contract.methodAnnotation())
            ));
            return;
        }
        if (methods.size() > 1) {
            String methodNames = methods.stream()
                    .map(method -> method.getSimpleName().toString())
                    .collect(Collectors.joining(", "));
            error(type, String.format(
                    "Expected exactly one @%s method in '%s', found %d: [%s].",
                    simpleName(contract.methodAnnotation()),
                    type.getQualifiedName(),
                    methods.size(),
                    methodNames
            ));
            return;
        }
        checkMethodSignature(methods.getFirst(), contract);
    }

    private void checkMethodSignature(ExecutableElement method, Contract contract) {
        if (contract.method() instanceof SubscribeContract(String receiverType)) {
            checkReceiver(method, receiverType);
        } else if (contract.method() instanceof FactoryContract(String factoryType)) {
            checkFactory(method, factoryType);
        }
    }

    private void checkReceiver(ExecutableElement method, String receiverTypeName) {
        var parameters = method.getParameters();
        if (parameters.size() != 1) {
            error(method, String.format(
                    "Method '%s' in '%s' has %d parameter(s), expected exactly 1 of type '%s'.",
                    method.getSimpleName(),
                    method.getEnclosingElement(),
                    parameters.size(),
                    receiverTypeName
            ));
            return;
        }
        TypeElement receiverType = processingEnv.getElementUtils().getTypeElement(receiverTypeName);
        if (receiverType == null) {
            error(method, String.format(
                    "registry-lib processor cannot resolve registration receiver '%s'.",
                    receiverTypeName
            ));
            return;
        }
        if (!processingEnv.getTypeUtils().isSameType(parameters.getFirst().asType(), receiverType.asType())) {
            error(method, String.format(
                    "The first parameter of '%s' in '%s' must be '%s'.",
                    method.getSimpleName(),
                    method.getEnclosingElement(),
                    receiverTypeName
            ));
        }
    }

    private void checkFactory(ExecutableElement method, String factoryTypeName) {
        if (!method.getParameters().isEmpty()) {
            error(method, String.format(
                    "Factory method '%s' in '%s' must have no parameters.",
                    method.getSimpleName(),
                    method.getEnclosingElement()
            ));
            return;
        }
        TypeElement factoryType = processingEnv.getElementUtils().getTypeElement(factoryTypeName);
        if (factoryType == null) {
            error(method, String.format(
                    "registry-lib processor cannot resolve factory type '%s'.",
                    factoryTypeName
            ));
            return;
        }
        if (!processingEnv.getTypeUtils().isAssignable(method.getReturnType(), processingEnv.getTypeUtils().erasure(factoryType.asType()))) {
            error(method, String.format(
                    "The return type of factory method '%s' in '%s' must implement '%s'.",
                    method.getSimpleName(),
                    method.getEnclosingElement(),
                    factoryTypeName
            ));
        }
    }

    private static boolean hasAnnotation(Element element, String annotationName) {
        return element.getAnnotationMirrors().stream()
                .anyMatch(mirror -> {
                    Element annotationElement = mirror.getAnnotationType().asElement();
                    return annotationElement instanceof TypeElement annotationType
                            && annotationType.getQualifiedName().contentEquals(annotationName);
                });
    }

    private static String simpleName(String qualifiedName) {
        int dot = qualifiedName.lastIndexOf('.');
        return dot == -1 ? qualifiedName : qualifiedName.substring(dot + 1);
    }

    private void error(Element element, String message) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, message, element);
    }
}
