plugins {
    `java-library`
    `maven-publish`
    signing
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

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
    withSourcesJar()
    withJavadocJar()
}

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

// Credentials are read from Gradle properties or environment variables so secrets stay out of VCS.
val mavenCentralUsername = providers.gradleProperty("mavenCentralUsername")
    .orElse(providers.environmentVariable("MAVEN_CENTRAL_USERNAME"))
    .orNull
val mavenCentralPassword = providers.gradleProperty("mavenCentralPassword")
    .orElse(providers.environmentVariable("MAVEN_CENTRAL_PASSWORD"))
    .orNull
val pomUrl = providers.gradleProperty("pom_url").orNull
val pomLicenseName = providers.gradleProperty("pom_license_name").orNull
val pomLicenseUrl = providers.gradleProperty("pom_license_url").orNull
val pomDeveloperId = providers.gradleProperty("pom_developer_id").orNull
val pomDeveloperName = providers.gradleProperty("pom_developer_name").orNull
val pomDeveloperEmail = providers.gradleProperty("pom_developer_email").orNull
val pomScmConnection = providers.gradleProperty("pom_scm_connection").orNull
val pomScmDeveloperConnection = providers.gradleProperty("pom_scm_developer_connection").orNull
val pomScmUrl = providers.gradleProperty("pom_scm_url").orNull

publishing {
    publications {
        register<MavenPublication>("mavenJava") {
            artifactId = "registry-lib"
            from(components["java"])
            pom {
                name = "registry-lib"
                description = "NeoForge annotation-driven registration helper library."
                url = pomUrl
                licenses {
                    license {
                        name = pomLicenseName
                        url = pomLicenseUrl
                        distribution = "repo"
                    }
                }
                developers {
                    developer {
                        id = pomDeveloperId
                        name = pomDeveloperName
                        if (!pomDeveloperEmail.isNullOrEmpty()) {
                            email = pomDeveloperEmail
                        }
                    }
                }
                scm {
                    connection = pomScmConnection
                    developerConnection = pomScmDeveloperConnection
                    url = pomScmUrl
                }
            }
        }
    }
    repositories {
        mavenLocal()
        maven {
            name = "CentralBundle"
            url = uri(layout.buildDirectory.dir("central-bundle"))
        }
        maven {
            name = "MavenCentralPortal"
            url = uri("https://central.sonatype.com/api/v1/publisher/upload")
            credentials {
                username = mavenCentralUsername
                password = mavenCentralPassword
            }
        }
    }
}

configure<SigningExtension> {
    // A PEM private key can be supplied in-memory; otherwise Gradle's standard signing properties apply.
    val signingKey = providers.gradleProperty("signingKey")
        .orElse(providers.environmentVariable("MAVEN_CENTRAL_SIGNING_KEY"))
        .orNull
    val signingPassword = providers.gradleProperty("signingPassword")
        .orElse(providers.environmentVariable("MAVEN_CENTRAL_SIGNING_PASSWORD"))
        .orNull
    isRequired = signingKey != null ||
            hasProperty("signing.keyId") ||
            hasProperty("signing.secretKeyRingFile") ||
            hasProperty("signing.gnupg.keyName")
    if (signingKey != null) {
        useInMemoryPgpKeys(signingKey, signingPassword)
    }
    sign(publishing.publications["mavenJava"])
}

idea {
    module {
        isDownloadSources = true
        isDownloadJavadoc = true
    }
}
