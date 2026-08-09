package com.mistbeyond.registry.impl;

import com.mistbeyond.registry.*;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforgespi.language.ModFileScanData;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.NoSuchElementException;

@Slf4j
public class CommonRegistryTable extends CommonRegistrar {
    private final BlockRegistration blockRegistration;
    private final ItemRegistration itemRegistration;
    private final MenuTypeRegistration menuTypeRegistration;
    private final ContainerScreenRegistration containerScreenRegistration;

    private boolean insensitiveProcessed = false;
    private boolean clientProcessed = false;

    /**
     * @param excludedPackagePrefixes retained for API compatibility; compile-time validation no longer needs it
     */
    @SuppressWarnings("unused")
    public CommonRegistryTable(
            String modId,
            DeferredRegister.Blocks blockRegister,
            DeferredRegister.Items itemRegister,
            DeferredRegister<BlockEntityType<?>> blockEntityRegister,
            DeferredRegister<MenuType<?>> menuRegister,
            List<String> excludedPackagePrefixes
    ) {
        super(modId, blockRegister, itemRegister, blockEntityRegister, menuRegister);
        blockRegistration = new BlockRegistration(modId, block, blockRegister);
        itemRegistration = new ItemRegistration(modId, item, itemRegister);
        menuTypeRegistration = new MenuTypeRegistration(modId, menuType, menuRegister);
        containerScreenRegistration = new ContainerScreenRegistration(modId, containerScreen);
    }

    /**
     * Gets factory from provider (usually it's the only annotated static method), no runtime checks.
     *
     * @param <F> the factory
     * @throws NoSuchElementException when there is no a factory provider.
     */
    @SuppressWarnings("unchecked")
    private static <F> F getFactory(Class<?> clazz) throws NoSuchElementException {
        try {
            return (F) ReflectHelper.getFirstAnnotatedStaticMethod(clazz, ProvideFactory.class).invoke(null);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Resolves every class annotated with {@code annotation} and invokes its unique
     * {@link SubscribeRegistration} static method with {@code receiver}.
     */
    private static <R> void invokeRegistration(ModFileScanData scanResult, Class<? extends Annotation> annotation, R receiver) {
        for (var data : ScanDataHelper.annotationDataOf(scanResult, annotation)) {
            invokeSubscribeRegistration(ScanDataHelper.resolve(data), receiver);
        }
    }

    /**
     * Invokes the unique {@link SubscribeRegistration} static method of {@code clazz} with {@code receiver}.
     */
    private static void invokeSubscribeRegistration(Class<?> clazz, Object receiver) {
        try {
            ReflectHelper.getFirstAnnotatedStaticMethod(clazz, SubscribeRegistration.class).invoke(null, receiver);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException(e);
        }
    }

    public void registerCommon(IEventBus modEventBus, ModContainer modContainer) {
        processSideInsensitive(modContainer);

        blockRegister.register(modEventBus);
        itemRegister.register(modEventBus);
        blockEntityRegister.register(modEventBus);
        menuRegister.register(modEventBus);
    }

    public void registerClient(IEventBus modEventBus, ModContainer modContainer) {
        processClientSide(modContainer);
    }

    private void processClientSide(ModContainer modContainer) {
        if (!clientProcessed) {
            var scanResult = modContainer.getModInfo()
                    .getOwningFile()
                    .getFile()
                    .getScanResult();
            processContainerScreen(scanResult);
            validateClientFamilyConsistency();
            clientProcessed = true;
        }
    }

    private void processSideInsensitive(ModContainer modContainer) {
        if (!insensitiveProcessed) {
            var scanResult = modContainer.getModInfo()
                    .getOwningFile()
                    .getFile()
                    .getScanResult();
            processBlock(scanResult);
            processItem(scanResult);
            processMenu(scanResult);
            processBlockEntity(scanResult);
            validateCommonFamilyConsistency();
            insensitiveProcessed = true;
        }
    }

    private void processBlock(ModFileScanData scanResult) {
        for (var data : ScanDataHelper.annotationDataOf(scanResult, RegisterBlock.class)) {
            var clazz = ScanDataHelper.resolve(data);
            var v = data.annotationData().get("registerBlockItem");
            blockRegistration.setAllowRegisteringBlockItem(v == null || (boolean) v);
            invokeSubscribeRegistration(clazz, blockRegistration);
        }
        blockRegistration.registerBlockItem(item, itemRegister);
    }

    private void processItem(ModFileScanData scanResult) {
        invokeRegistration(scanResult, RegisterItem.class, itemRegistration);
    }

    private void processMenu(ModFileScanData scanResult) {
        invokeRegistration(scanResult, RegisterMenuType.class, menuTypeRegistration);
    }

    private void processBlockEntity(ModFileScanData scanResult) {
        for (var data : ScanDataHelper.annotationDataOf(scanResult, RegisterBlockEntityType.class)) {
            var clazz = ScanDataHelper.resolve(data);
            var name = (String) data.annotationData().get("value");
            var id = Identifier.fromNamespaceAndPath(this.modId, name);
            if (this.blockEntityType.containsKey(id)) {
                throw new IllegalStateException("BlockEntityType '" + name + "' was registered twice.");
            }
            this.blockEntityType.put(id, blockEntityRegister.register(name, () -> new BlockEntityType<>(getFactory(clazz), block.get(id).value())));
        }
    }

    private void processContainerScreen(ModFileScanData scanResult) {
        invokeRegistration(scanResult, RegisterContainerScreen.class, containerScreenRegistration);
    }

    /**
     * Ensures every registered menu/block entity type id has a matching block registered under the same id.
     */
    private void validateCommonFamilyConsistency() {
        var report = new CheckReport();
        for (var id : menuType.keySet()) {
            if (!block.containsKey(id)) {
                report.addErrorMessage(String.format("MenuType '%s' has no matching Block registered.", id.getPath()));
            }
        }
        for (var id : blockEntityType.keySet()) {
            if (!block.containsKey(id)) {
                report.addErrorMessage(String.format("BlockEntityType '%s' has no matching Block registered.", id.getPath()));
            }
        }
        report.throwIfFailed(log);
    }

    /**
     * Ensures every registered container screen id has a matching menu type.
     */
    private void validateClientFamilyConsistency() {
        var report = new CheckReport();
        for (var id : containerScreen.keySet()) {
            if (!menuType.containsKey(id)) {
                report.addErrorMessage(String.format("ContainerScreen '%s' has no matching MenuType registered.", id.getPath()));
            }
        }
        report.throwIfFailed(log);
    }
}
