plugins {
    kotlin("jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.github.node-gradle.node")
    alias(ktorLibs.plugins.ktor)
    application
}



dependencies {
    implementation(ktorLibs.server.core)
    implementation(ktorLibs.server.cio)
    implementation(ktorLibs.server.statusPages)
    implementation(ktorLibs.server.htmlBuilder)
    implementation(ktorLibs.server.defaultHeaders)
    implementation(ktorLibs.server.cachingHeaders)
    implementation(ktorLibs.server.conditionalHeaders)
    implementation(ktorLibs.server.auth.jwt)
    implementation(ktorLibs.client.core)
    implementation(ktorLibs.client.cio)
    implementation(ktorLibs.client.contentNegotiation)
    implementation(ktorLibs.serialization.kotlinx.json)

    "1.3.1".let { version ->
        implementation("org.jetbrains.exposed:exposed-core:${version}")
        implementation("org.jetbrains.exposed:exposed-jdbc:${version}")
        implementation("org.jetbrains.exposed:exposed-json:${version}")
        implementation("org.jetbrains.exposed:exposed-java-time:${version}")
    }

    implementation("org.postgresql:postgresql:42.7.12")
    implementation("com.zaxxer:HikariCP:5.1.0")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.8.0")

    implementation("ch.qos.logback:logback-classic:1.5.13")
    implementation("org.jetbrains:markdown:0.1.45")
    implementation("org.mindrot:jbcrypt:0.4")
    implementation(kotlin("stdlib"))


}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-parameters")
    }
}

node {
    version.set("26.3.0")
}


tasks.register<com.github.gradle.node.npm.task.NpxTask>("tailwind") {
    dependsOn("npmInstall")
    command.set("@tailwindcss/cli")

    args.set(
        listOf(
            "-i", "./src/main/resources/static/style.css",
            "-o", "./build/resources/main/static/style.css",
            "--minify"
        )
    )
}

tasks.named("classes") {
    dependsOn("tailwind")
}


application {
    mainClass = "dev.metinkale.halalapp.LauncherKt"
}
