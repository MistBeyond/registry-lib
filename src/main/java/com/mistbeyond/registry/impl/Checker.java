package com.mistbeyond.registry.impl;

/**
 * Runs a registration check against a {@link ClassContainer} and reports failures to a {@link CheckReport}.
 */
@FunctionalInterface
@Deprecated
public interface Checker {
    /**
     * Runs this checker.
     */
    void check();
}