package com.mistbeyond.registry.impl;

import net.neoforged.neoforgespi.language.ModFileScanData;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.util.List;

/**
 * Access to the NeoForge {@link ModFileScanData} API.
 */
class ScanDataHelper {
    private ScanDataHelper() {
    }

    /**
     * Returns the annotation data of the classes annotated with {@code annotation} (type-level only).
     */
    public static List<ModFileScanData.AnnotationData> annotationDataOf(ModFileScanData scanData, Class<? extends Annotation> annotation) {
        return scanData.getAnnotatedBy(annotation, ElementType.TYPE).toList();
    }

    /**
     * Resolves the annotated member as a loaded class.
     * <p>
     * The subtype contract is validated by {@code RegistryProcessor} at compile time.
     */
    public static Class<?> resolve(ModFileScanData.AnnotationData data) {
        return ReflectHelper.loadClasses(List.of(data.memberName())).getFirst();
    }
}
