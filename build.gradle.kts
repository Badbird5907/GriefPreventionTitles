plugins {
    java
    id("io.github.goooler.shadow") version "8.1.8"
}
group = "dev.badbird"
version = "1.2.0"
val jarName = "GriefPreventionTitles"

repositories {
    mavenCentral()
    mavenLocal()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://oss.sonatype.org/content/groups/public/")
    maven("https://jitpack.io")
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.20.1-R0.1-SNAPSHOT")
    compileOnly("com.github.GriefPrevention:GriefPrevention:16.18.2")
    implementation("net.kyori:adventure-platform-bukkit:4.3.3")
    implementation("net.kyori:adventure-api:4.17.0")
    implementation("net.kyori:adventure-text-minimessage:4.17.0")
}
val targetJavaVersion = 17
java {
    val javaVersion = JavaVersion.toVersion(targetJavaVersion)
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
    if (JavaVersion.current() < javaVersion) {
        toolchain.languageVersion.set(JavaLanguageVersion.of(targetJavaVersion))
    }
}
tasks.withType<JavaCompile>().configureEach {
    if (targetJavaVersion >= 10 || JavaVersion.current().isJava10Compatible()) {
        options.release.set(targetJavaVersion)
    }
}
tasks.withType<ProcessResources> {
    filesMatching("plugin.yml") {
        expand(project.properties)
    }
}
tasks.create<Copy>("copyPlugin") {
    from("build/libs/$jarName.jar")
    into("run/plugins")
}
tasks.getByName("copyPlugin").dependsOn(tasks.getByName("shadowJar"))

tasks {
    shadowJar {
        archiveFileName.set("$jarName.jar")

        exclude("*.txt")
        exclude("*.md")
        exclude("*.md")
        exclude("LICENSE")
        exclude("AUTHORS")
    }
    build {
        dependsOn(shadowJar)
    }
}