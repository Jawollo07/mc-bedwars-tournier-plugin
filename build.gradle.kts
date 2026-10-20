plugins {
    java
}

group = "de.turnier"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    // Falls die Auflösung scheitert: feste Version nutzen, z. B. 26.3.build.143-beta
    compileOnly("io.papermc.paper:paper-api:26.3.build.143-beta")
    // MBedwars-API (nur zum Kompilieren, wird auf dem Server vom MBedwars-Plugin gestellt)
    compileOnly(files("libs/MBedwars-5_5_9_1.jar"))
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
        options.release.set(25)
    }
    processResources {
        filesMatching("plugin.yml") {
            expand("version" to project.version)
        }
    }
}
