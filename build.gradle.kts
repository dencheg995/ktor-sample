import com.bmuschko.gradle.docker.tasks.container.DockerCreateContainer
import com.bmuschko.gradle.docker.tasks.container.DockerExistingContainer
import com.bmuschko.gradle.docker.tasks.container.DockerStartContainer
import com.bmuschko.gradle.docker.tasks.container.DockerStopContainer
import com.bmuschko.gradle.docker.tasks.image.DockerPullImage
import com.github.dockerjava.api.async.ResultCallback
import com.github.dockerjava.api.model.Frame
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.flywaydb.gradle.task.FlywayMigrateTask
import java.io.Closeable
import java.util.concurrent.CountDownLatch


val ktor_version: String by project
val kotlin_version: String by project
val logback_version: String by project
val jooqVersion = "3.14.4"


plugins {
    application
    kotlin("jvm") version "1.4.32"
    id("nu.studer.jooq") version "5.2"
    id("org.flywaydb.flyway") version "7.3.2"
    id("com.bmuschko.docker-remote-api") version "6.6.1"
    id("com.github.johnrengelman.shadow") version "6.1.0"
    kotlin("plugin.serialization") version "1.4.10"
}

group = "com.example"
version = "0.0.1"
application {
    mainClass.set("com.example.ApplicationKt")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation ("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlin_version")
    implementation("io.ktor:ktor-server-core:$ktor_version")
    implementation("io.ktor:ktor-server-netty:$ktor_version")
    implementation("io.ktor:ktor-server-core:$ktor_version") // ktor server
    implementation("io.ktor:ktor-gson:$ktor_version")
    implementation("ch.qos.logback:logback-classic:$logback_version")
    implementation("com.zaxxer:HikariCP:3.4.5")
    implementation("org.postgresql:postgresql:42.2.1")
    testImplementation("io.ktor:ktor-server-tests:$ktor_version")
    implementation("org.flywaydb:flyway-core:7.3.2")
    implementation("org.jooq:jooq:$jooqVersion")
    jooqGenerator("org.postgresql:postgresql:42.2.1")
    implementation("io.ktor:ktor-serialization:$ktor_version")


}

val pullDbImage by tasks.creating(DockerPullImage::class) {
    image.set("postgres:11")
}

val createDbContainer by tasks.creating(DockerCreateContainer::class) {
    dependsOn(pullDbImage)
    targetImageId(pullDbImage.image)
    containerName.set("postgres-jooq")
    hostConfig.portBindings.add("15432:5432")
    envVars.put("POSTGRES_PASSWORD", "secret")
    hostConfig.autoRemove.set(true)
}

val startDbContainer by tasks.creating(DockerStartContainer::class) {
    dependsOn(createDbContainer)
    targetContainerId(createDbContainer.containerId)
}

val waitDbContainer by tasks.creating(WaitDbContainer::class) {
    dependsOn(startDbContainer)
    targetContainerId(createDbContainer.containerId)
}

val stopDbContainer by tasks.creating(DockerStopContainer::class) {
    targetContainerId(createDbContainer.containerId)
}

val migrateDb by tasks.creating(FlywayMigrateTask::class) {
    dependsOn(waitDbContainer)
    url = "jdbc:postgresql://localhost:15432/postgres"
    user = "postgres"
    password = "secret"
}

jooq {
    version.set(jooqVersion)
    configurations {
        create("main") {
            jooqConfiguration.apply {
                logging = org.jooq.meta.jaxb.Logging.WARN
                jdbc.apply {
                    driver = "org.postgresql.Driver"
                    url = "jdbc:postgresql://localhost:15432/postgres"
                    user = "postgres"
                    password = "secret"
                }
                generator.apply {
                    name = "org.jooq.codegen.KotlinGenerator"
                    database.apply {
                        name = "org.jooq.meta.postgres.PostgresDatabase"
                        inputSchema = "public"
                    }
                    target.apply {
                        packageName = "catalogservice.jooq"
                        directory = "build/generated-src/jooq/main"
                    }
                    strategy.name = "org.jooq.codegen.DefaultGeneratorStrategy"
                }
            }
        }
    }
}

val generateJooq by tasks
generateJooq.dependsOn(migrateDb)
generateJooq.finalizedBy(stopDbContainer)

val shadowJar: ShadowJar by tasks
shadowJar.manifest.attributes["Main-Class"] = "catalogservice.ApplicationKt"
shadowJar.archiveVersion.set("")

val build by tasks
build.dependsOn(shadowJar)

buildscript {
    configurations["classpath"].resolutionStrategy.eachDependency {
        if (requested.group == "org.jooq") {
            useVersion("3.14.4")
        }
    }
}

open class WaitDbContainer : DockerExistingContainer() {
    override fun runRemoteCommand() {
        dockerClient.logContainerCmd(containerId.get()).run {
            withTailAll()
            withFollowStream(true)
            withStdOut(true)
            withStdErr(true)
            val serverStarted = CountDownLatch(1)
            exec(object : ResultCallback<Frame> {
                override fun close() {
                }

                override fun onStart(p0: Closeable?) {
                }

                override fun onNext(p0: Frame?) {
                    if (p0.toString().contains("""listening on IPv4 address "0.0.0.0", port 5432"""))
                        serverStarted.countDown()
                }

                override fun onError(p0: Throwable?) {
                }

                override fun onComplete() {
                }
            })
            serverStarted.await()
        }
    }
}
