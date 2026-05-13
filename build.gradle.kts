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
    maven { url = uri("https://maven.aliyun.com/repository/public") }
    mavenCentral()
}

val awsSdkVersion = "2.44.4"

dependencyManagement {
    imports {
        mavenBom("software.amazon.awssdk:bom:$awsSdkVersion")
    }
}

dependencies {
    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-data-mongodb")
    implementation("org.springframework.boot:spring-boot-starter-aop")

    // Swagger UI (springdoc-openapi for Spring Boot 2.x)
    implementation("org.springdoc:springdoc-openapi-ui:1.6.15")

    // FAISS JNI (default vector engine, Linux only)
    // compileOnly: 仅编译期使用，运行期在 Linux 环境通过 runtimeOnly 或 implementation 引入
    compileOnly("com.criteo.jfaiss:jfaiss-cpu:1.7.0-1")

    // AWS S3
    implementation("software.amazon.awssdk:s3")

    // Rate limiter & utilities
    implementation("com.google.guava:guava:33.4.0-jre")
    implementation("cn.hutool:hutool-core:5.8.34")

    // Lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testCompileOnly("org.projectlombok:lombok")
    testAnnotationProcessor("org.projectlombok:lombok")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
