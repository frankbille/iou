import org.springframework.boot.gradle.tasks.run.BootRun

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinJpa)
    alias(libs.plugins.kotlinSpring)
    alias(libs.plugins.springDependencyManagement)
    alias(libs.plugins.springBoot)
}

group = "dk.frankbille"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(24)
    }
}

repositories {
    mavenCentral()
}

val mainSourceSet = sourceSets.named("main").get()
val devSourceSet =
    sourceSets.create("dev") {
        kotlin.setSrcDirs(listOf("src/dev/kotlin"))
        resources.setSrcDirs(listOf("src/dev/resources"))
        compileClasspath += mainSourceSet.output
        runtimeClasspath += output + mainSourceSet.output
    }

configurations.named(devSourceSet.implementationConfigurationName) {
    extendsFrom(configurations.named("testImplementation").get())
}
configurations.named(devSourceSet.compileOnlyConfigurationName) {
    extendsFrom(configurations.named("testCompileOnly").get())
}
configurations.named(devSourceSet.runtimeOnlyConfigurationName) {
    extendsFrom(configurations.named("testRuntimeOnly").get())
}

dependencies {
    implementation(libs.aspectjweaver)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.graphql)
    implementation(libs.spring.boot.starter.liquibase)
    implementation(libs.spring.boot.starter.oauth2.resource.server)
    implementation(libs.spring.boot.starter.security)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.webmvc)
    implementation(libs.kotlin.reflect)
    implementation(libs.kotlinx.datetime)
    implementation(libs.jackson.module.kotlin)
    implementation(libs.graphql.java.extended.scalars)

    implementation(projects.shared)

    runtimeOnly(libs.mysql.connector.j)

    testImplementation(libs.spring.boot.devtools)
    testImplementation(libs.spring.boot.starter.data.jpa.test)
    testImplementation(libs.spring.boot.starter.graphql.test)
    testImplementation(libs.spring.boot.starter.liquibase.test)
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.starter.webmvc.test)
    testImplementation(libs.spring.boot.testcontainers)
    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.reactor.test)
    testImplementation(libs.spring.security.test)
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.testcontainers.mysql)
    testImplementation(libs.assertj.core)
    testImplementation(libs.p6spy.spring.boot.starter)

    testRuntimeOnly(libs.junit.platform.launcher)
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
    }
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.register<BootRun>("dev") {
    group = "application"
    description = "Runs the local development server."
    dependsOn(tasks.named(devSourceSet.classesTaskName))
    classpath = devSourceSet.runtimeClasspath
    mainClass.set("dk.frankbille.iou.dev.DevIouApiApplicationKt")
}
