package com.mistbeyond.registry;

import com.mistbeyond.registry.impl.ContainerScreenRegistration;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a container screen class for auto-registration in the mod's registry system.
 * <p>Contract validation is performed at compile time by {@code RegistryProcessor}.
 * The annotated class must contain exactly one static method, and strictly requires extending {@link net.minecraft.client.gui.screens.inventory.AbstractContainerScreen AbstractContainerScreen}.
 * The static method must be annotated with {@link SubscribeRegistration}, and its only parameter must be {@link ContainerScreenRegistration}
 * <p>
 * Then, you can register container screens like this:
 * <pre>{@code
 * @SubscribeRegistration
 * private static void registerScreens(ContainerScreenRegistration registration) {
 *     registration.register("your_screen_name", YourScreen::new);
 * }}</pre>
 * <p>
 * Registering a subclass instance from the superclass registration is not recommended; this cannot be validated at compile time.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface RegisterContainerScreen {
}
