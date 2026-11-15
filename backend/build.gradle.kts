plugins {
    java
    id("org.springframework.boot") version "4.1.2"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "engine"
version = providers.environmentVariable("BUILD_ID").getOrElse("dev")

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
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation("org.apache.commons:commons-compress:1.27.1")
    implementation("com.anthropic:anthropic-java:3.3.0")
    implementation("com.openai:openai-java:4.101.0")
    runtimeOnly("org.postgresql:postgresql")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("com.tngtech.archunit:archunit-junit5:1.5.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<JavaCompile> {
    options.compilerArgs.addAll(listOf("-Xlint:all,-processing", "-Werror"))
}

// Starting and using generated software belongs to real runs; a test that wants it turns it on.
tasks.withType<Test> {
    systemProperty("engine.execution.use-check", "false")
}

tasks.test {
    description = "Unit and architecture tests; no database needed."
    useJUnitPlatform { excludeTags("integration") }
}

val integrationTest by tasks.registering(Test::class) {
    description = "Tests that need the PostgreSQL of ENGINE_DB_URL."
    group = "verification"
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    useJUnitPlatform { includeTags("integration") }
    shouldRunAfter(tasks.test)
    doFirst {
        val url = System.getenv("ENGINE_DB_URL") ?: "jdbc:postgresql://localhost:5432/engine"
        val database = url.substringAfterLast('/').substringBefore('?')
        val main = System.getenv("ENGINE_DB_NAME") ?: "engine"
        if (database == "engine" || database == main) {
            throw GradleException(
                "Refusing to run the integration tests on the database '$database': it is the main " +
                    "engine database and the tests would fill it with projects. Set ENGINE_DB_URL to a " +
                    "test database, for example jdbc:postgresql://localhost:5432/engine_test " +
                    "(ci/run test-integration does this)."
            )
        }
    }
}

springBoot {
    buildInfo()
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
    inputs.dir(frontendDir.dir("static"))
    inputs.files(
        frontendDir.file("package.json"),
        frontendDir.file("vite.config.ts"),
    )
    outputs.dir(frontendDir.dir("build"))
}

tasks.processResources {
    from(frontendBuild) { into("static") }
}

val printJavaHome by tasks.registering {
    description = "Prints the Java 21 home of the toolchain; ci/run starts the jar with it."
    val launcher = javaToolchains.launcherFor(java.toolchain)
    doLast { println(launcher.get().metadata.installationPath.asFile.absolutePath) }
}
