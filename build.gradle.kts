plugins {
    kotlin("jvm") version "2.1.20"
    `java-library`
    id("com.ncorti.ktfmt.gradle") version "0.22.0"
}

group = "org.sunsetware.omio"

version = "0.1.0"

repositories { mavenCentral() }

dependencies {
    testImplementation(kotlin("test"))
    testImplementation(platform("org.junit:junit-bom:5.12.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core:3.27.2")
    testImplementation("commons-io:commons-io:2.19.0")
    testImplementation("com.jayway.jsonpath:json-path:2.9.0")
    // for suppressing jsonpath log warnings
    testImplementation("org.slf4j:slf4j-nop:2.0.17")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
    testLogging { events("passed", "skipped", "failed") }
}

ktfmt { kotlinLangStyle() }
