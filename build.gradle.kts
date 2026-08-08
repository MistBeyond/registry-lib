plugins {
    `java-library`
    `maven-publish`
    idea
    id("net.neoforged.moddev") version ("2.0.141")
    id("io.freefair.lombok") version "9.2.0"
}

val neoVersion = findProperty("neo_version") as String

group = findProperty("mod_group_id") as String
version = findProperty("mod_version") as String

base {
    archivesName = "registry-lib"
}

java.toolchain.languageVersion = JavaLanguageVersion.of(25)

repositories {
    mavenCentral()
}

neoForge {
    version = neoVersion
}

dependencies {
    compileOnly("org.jspecify:jspecify:1.0.0")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

publishing {
    publications {
        register<MavenPublication>("mavenJava") {
            artifactId = "registry-lib"
            artifact(tasks.named("jar"))
            pom {
                name = "registry-lib"
                description = "NeoForge annotation-driven registration helper library."
            }
        }
    }
    repositories {
        mavenLocal()
    }
}

idea {
    module {
        isDownloadSources = true
        isDownloadJavadoc = true
    }
}
