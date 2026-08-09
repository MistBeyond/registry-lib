package com.mistbeyond.registry;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The annotated static method must have no parameters, and return a type that implements the factory.
 * <p>Contract validation is performed at compile time by {@code RegistryProcessor}.
 * <p>
 * You can provide the factory like this:
 * <pre>{@code
 * @ProvideFactory
 * private static Factory provideFactory() {
 *    return YourClass::new;
 * }
 * }</pre>
 * <p>
 * Providing the subclass factory in the superclass is not recommended; this cannot be validated at compile time.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ProvideFactory {
}
