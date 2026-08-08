# Design Principles

## 1. Public APIs are interfaces

Consumers depend on contracts instead of concrete classes. Implementation classes live in an `impl` subpackage and
stay package-private where possible. Framework integration may still require concrete types.

## 2. Dependencies stay acyclic

The library only depends on Minecraft/NeoForge, JDK, and small provided libraries such as Guava, SLF4J, and JSpecify.
It never imports consuming mod packages.

## 3. Prefer composition to inheritance

Build behavior from fields and helper components; extend NeoForge/Minecraft classes only when the framework requires
it.

## 4. Prefer immutable data and explicit nullability

Every Java package has a `package-info.java` annotated with JSpecify `@NullMarked`. Avoid null literals and shared
mutable state in new code.

## 5. Cross-feature access goes through public APIs

Never reach into `impl` from consumer code unless the class is explicitly documented as a public early-check API.
