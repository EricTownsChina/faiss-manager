plugins {
    java
    id("org.springframework.boot") version "2.7.14"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "io.github.erictowns"
version = "0.0.1-SNAPSHOT"
description = "faiss-manager"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testCompileOnly("org.projectlombok:lombok")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testAnnotationProcessor("org.projectlombok:lombok")
}

dependencyManagement {
}

tasks.withType<Test> {
    useJUnitPlatform()
}
