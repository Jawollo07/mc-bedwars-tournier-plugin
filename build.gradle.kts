plugins {
    java
}

group = "de.turnier"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.marcely.de/repository/maven-public/")
}

dependencies {
    // Paper wird vom Server bereitgestellt.
    compileOnly("io.papermc.paper:paper-api:26.3.build.143-beta")

    // Offizielle MBedwars-API aus dem Maven-Repository; keine lokale JAR nötig.
    compileOnly("de.marcely.bedwars:API:5.5.9")

    // Für Unit-Tests die API-Klassen auf dem Laufzeit-Classpath verfügbar machen.
    testRuntimeOnly("io.papermc.paper:paper-api:26.3.build.143-beta")
    testRuntimeOnly("de.marcely.bedwars:API:5.5.9")
    testImplementation("org.junit.jupiter:junit-jupiter:5.12.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.12.2")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
        options.release.set(25)
    }
    test {
        useJUnitPlatform()
    }
    processResources {
        filesMatching("plugin.yml") {
            expand("version" to project.version)
        }
    }
}
