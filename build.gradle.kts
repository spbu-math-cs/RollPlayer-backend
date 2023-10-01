plugins {
    kotlin("jvm") version "1.8.0"
    application
<<<<<<< HEAD
    id("com.google.protobuf") version "0.8.17"
=======
>>>>>>> eda4491e77cfc0b840c7bb77e18cce92400814dc
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

<<<<<<< HEAD
val ktor_version = "1.6.2"

dependencies {
    implementation("io.ktor:ktor-server-netty:$ktor_version")

    implementation("io.ktor:ktor-websockets:2.0.0")
    implementation("io.grpc:grpc-protobuf:1.40.1")
    implementation("io.ktor:ktor-server-websockets:2.0.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.0")
    implementation("io.ktor:ktor-server-core:1.4.1")
    implementation("io.ktor:ktor-server-netty:2.3.4")
    implementation("org.jetbrains.kotlinx:kotlinx-io-core:0.3.0")
    implementation("io.ktor:ktor-server-content-negotiation:2.0.1")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.0.1")
    testImplementation("io.ktor:ktor-server-tests:$ktor_version")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.7.0")
//    implementation("io.ktor:ktor-jackson:1.6.10")
//    implementation("io.ktor:ktor-config:1.6.10")
=======
val exposedVersion = "0.40.1"

dependencies {
    testImplementation(kotlin("test"))

    implementation("com.h2database:h2:2.1.214")
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")
>>>>>>> eda4491e77cfc0b840c7bb77e18cce92400814dc

    runtimeOnly("org.slf4j:slf4j-simple:1.7.32")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(8)
}

<<<<<<< HEAD

=======
>>>>>>> eda4491e77cfc0b840c7bb77e18cce92400814dc
application {
    mainClass.set("MainKt")
}