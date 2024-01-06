plugins {
    java
    kotlin("jvm") version "1.9.21"
    `maven-publish`
}

group = "com.johnsonoxr.ex-number"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.jetbrains.kotlin:kotlin-test")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "com.johnsonoxr"
            artifactId = "ex-number"
            version = "1.0.0"

            from(components["java"])
        }
    }
}