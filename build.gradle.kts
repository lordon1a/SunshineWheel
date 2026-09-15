plugins {
    java
}

group = "me.guldal"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven("https://jitpack.io")
}

dependencies {
    // Geniş uyumluluk için en eski desteklenen sürüme karşı derlenir (Paper 1.21+).
    compileOnly("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")
    compileOnly("me.clip:placeholderapi:2.11.6")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7.1")
}

// Alıcılar Java 21+ kullanır; bu bilgisayardaki JDK 26 ile Java 21 hedefli derliyoruz.
tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(21)
}

tasks.processResources {
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand("version" to project.version)
    }
}

// Release paketi: jar + README + LICENSE + CHANGELOG (sunucu-özel dosya girmez).
tasks.register<Zip>("releaseZip") {
    dependsOn(tasks.jar)
    archiveBaseName.set("SunshineWheel-release")
    archiveVersion.set(project.version.toString())
    from(layout.projectDirectory) {
        include("README.md", "LICENSE", "CHANGELOG.md")
    }
    from(layout.projectDirectory.dir("web")) {
        into("web")
    }
    from(tasks.jar) {
        into("plugin")
    }
    destinationDirectory.set(layout.buildDirectory.dir("distributions"))
}

// Yerel test sunucusuna kopyala: gradlew deployLocal
tasks.register<Copy>("deployLocal") {
    dependsOn(tasks.jar)
    from(tasks.jar)
    into(layout.projectDirectory.dir("run/plugins"))
}
