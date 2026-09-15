plugins {
    java
}

group = "me.guldal"
version = "0.1.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.2.build.123-stable")
}

// Canlı sunucu Java 25 ile çalışıyor; bu bilgisayardaki JDK 26 ile Java 25 hedefli derliyoruz.
tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(25)
}

tasks.processResources {
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand("version" to project.version)
    }
}

// Yerel test sunucusuna kopyala: gradlew deployLocal
tasks.register<Copy>("deployLocal") {
    dependsOn(tasks.jar)
    from(tasks.jar)
    into(layout.projectDirectory.dir("run/plugins"))
}
