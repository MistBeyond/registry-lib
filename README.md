# registry-lib

A small NeoForge helper library for annotation-driven registration of blocks, items, block entities, menu types, and
container screens.

The library ships `RegistryProcessor`, a Java annotation processor registered through `META-INF/services`. Add the
artifact to the annotation processor path of the consuming mod so registration contracts fail at compile time instead
of being checked through reflection at runtime:

```gradle
annotationProcessor "com.mistbeyond:registry-lib:1.1.0"
```

Publish locally with `gradlew publishToMavenLocal` and consume as
`com.mistbeyond:registry-lib:1.1.0`.
