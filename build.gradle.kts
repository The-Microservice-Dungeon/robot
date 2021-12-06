import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "2.5.5"
    id("io.spring.dependency-management") version "1.0.11.RELEASE"
    id("org.jetbrains.kotlin.plugin.allopen") version "1.5.31"
    id("org.jetbrains.kotlin.plugin.noarg") version "1.5.31"
    id("org.jlleitschuh.gradle.ktlint") version "10.2.0"
    id("com.google.cloud.tools.jib") version "3.1.4"
    id("org.jetbrains.dokka") version "1.5.31"
    jacoco
    kotlin("jvm") version "1.5.31"
    kotlin("plugin.spring") version "1.5.31"
    kotlin("plugin.jpa") version "1.5.31"
    kotlin("kapt") version "1.5.10"
}

group = "com.msd"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_11

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-jpa:2.5.5")
    implementation("org.springframework.boot:spring-boot-starter-web:2.5.5")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.0")
    implementation("org.apache.kafka:kafka-streams:2.8.0")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.springframework.kafka:spring-kafka:2.7.6")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("io.github.microutils:kotlin-logging-jvm:2.1.0")
    testImplementation("com.squareup.okhttp3:okhttp:4.9.2")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.9.2")
    implementation("org.mapstruct:mapstruct:1.5.0.Beta1")
    kapt("org.mapstruct:mapstruct-processor:1.5.0.Beta1")
    testImplementation(platform("org.junit:junit-bom:5.8.1"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.8.1")
    runtimeOnly("com.h2database:h2")
    testImplementation("org.springframework.boot:spring-boot-starter-test:2.5.5")
    testImplementation("org.springframework.kafka:spring-kafka-test:2.7.6")
    testImplementation("io.mockk:mockk:1.12.0")
}

kapt {
    arguments {
        // Set Mapstruct Configuration options here
        // https://kotlinlang.org/docs/reference/kapt.html#annotation-processor-arguments
        // https://mapstruct.org/documentation/stable/reference/html/#configuration-options
        arg("mapstruct.defaultComponentModel", "spring")
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "11"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test) // tests are required to run before generating the report
}

jacoco {
    // You may modify the Jacoco version here
    toolVersion = "0.8.7"
}

configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
    disabledRules.set(setOf("no-wildcard-imports"))
}
