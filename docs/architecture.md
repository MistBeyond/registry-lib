# Architecture Map

This document is the authoritative source for package placement in `registry-lib`. Read it before any structural
change: new packages, moving or extracting classes, or changing dependency direction.

## Package map

| Package                    | Responsibility                                                            |
|----------------------------|--------------------------------------------------------------------------|
| `com.mistbeyond.registry`  | Public registration contracts and annotations                             |
| `com.mistbeyond.registry.impl` | Implementations and internal helpers; consumers should not reach into it |

## Rules

- Public APIs stay in `com.mistbeyond.registry`; implementations go in `com.mistbeyond.registry.impl`.
- `impl` classes that are used by external early-check tooling (`Checks`, `CheckReport`, `ClassContainer`) remain
  public, but the rest of the implementation is internal.
- The library must not depend on any consuming mod's packages. Mod-specific scan exclusions are supplied by the
  consumer through `CommonRegistrar.of(..., excludedPackagePrefixes)`.
