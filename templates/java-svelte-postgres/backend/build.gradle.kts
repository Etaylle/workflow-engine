plugins {
    java
    id("org.springframework.boot") version "4.1.2"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "app"
version = "0.1.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:postgresql")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<JavaCompile> {
    options.compilerArgs.addAll(listOf("-Xlint:all,-processing", "-Werror"))
}

tasks.test {
    useJUnitPlatform()
}

val frontendDir = layout.projectDirectory.dir("../frontend")

val frontendInstall by tasks.registering(Exec::class) {
    description = "Installs the npm packages of the frontend."
    workingDir = frontendDir.asFile
    commandLine("npm", "ci")
    inputs.files(frontendDir.file("package.json"), frontendDir.file("package-lock.json"))
    outputs.dir(frontendDir.dir("node_modules"))
}

val frontendBuild by tasks.registering(Exec::class) {
    description = "Builds the static frontend."
    dependsOn(frontendInstall)
    workingDir = frontendDir.asFile
    commandLine("npm", "run", "build")
    inputs.dir(frontendDir.dir("src"))
    inputs.files(frontendDir.file("package.json"), frontendDir.file("index.html"))
    outputs.dir(frontendDir.dir("dist"))
}

tasks.processResources {
    from(frontendBuild) { into("static") }
}
