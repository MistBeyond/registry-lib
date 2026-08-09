package com.mistbeyond.registry;

import com.mistbeyond.registry.impl.BlockRegistration;
import com.mistbeyond.registry.impl.ContainerScreenRegistration;
import com.mistbeyond.registry.impl.ItemRegistration;
import com.mistbeyond.registry.impl.MenuTypeRegistration;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The annotated method must be static, and its only parameter must be a registration receiver,
 * such as {@link BlockRegistration}, {@link ItemRegistration}, {@link MenuTypeRegistration} or {@link ContainerScreenRegistration}.
 * <p>Contract validation is performed at compile time by {@code RegistryProcessor}.
 * <p>
 * Then, you can register objects, like blocks:
 * <pre>{@code
 * @SubscribeRegistration
 * private static void registerBlocks(BlockRegistration registration) {
 *     // if your block constructor is the standard (Properties) -> YourBlock
 *     registration.register("your_block_name", YourBlock::new);
 *     // else you can
 *     registration.register("your_block_name", p -> new YourBlock(p, otherParams));
 *     // you can also set the property
 *     registration.register("your_block_name", YourBlock::new, p -> p.strength(1.0f));
 * }}</pre>
 * <p>
 * Registering a subclass instance from the superclass registration is not recommended; this cannot be validated at compile time.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface SubscribeRegistration {
}
