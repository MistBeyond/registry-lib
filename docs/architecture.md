# Architecture Map

This document is the authoritative source for package placement in `registry-lib`. Read it before any structural
change: new packages, moving or extracting classes, or changing dependency direction.

## Package map

| Package                    | Responsibility                                                            |
|----------------------------|--------------------------------------------------------------------------|
| `com.mistbeyond.registry`  | Public registration contracts and annotations                             |
| `com.mistbeyond.registry.impl` | Implementations and internal helpers; consumers should not reach into it |
| `com.mistbeyond.registry.impl.processor` | Compile-time annotation processor (`RegistryProcessor`)                  |

## Rules

- Public APIs stay in `com.mistbeyond.registry`; implementations go in `com.mistbeyond.registry.impl`.
- Legacy runtime check helpers (`Checks`, `ClassContainer`, and the checker classes) remain public for compatibility
  but are deprecated; `CheckReport` is still used by the remaining family-consistency checks. New contract validation
  is performed by `RegistryProcessor` at compile time.
- The library must not depend on any consuming mod's packages. The deprecated
  `CommonRegistrar.of(..., excludedPackagePrefixes)` overload is kept for binary compatibility but is ignored.
