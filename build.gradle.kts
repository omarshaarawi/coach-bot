import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jooq.meta.jaxb.ForcedType
import org.jooq.meta.jaxb.Logging

plugins {
    application
    idea

    alias(libs.plugins.kotlin)
    alias(libs.plugins.ktor)
    alias(libs.plugins.jooq)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
    alias(libs.plugins.kotlinSerialization)
}

group = "com.football.fantasy"
application {
    mainClass.set("com.fantasy.football.ApplicationKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
    maven {
        url = uri("https://maven.pkg.github.com/omarshaarawi/fantasy-football-jvm")
        credentials {
            username = project.findProperty("gpr.user") as String? ?: System.getenv("GIT_USERNAME")
            password = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
        }
    }
    mavenCentral()
    maven("https://jitpack.io")
    mavenLocal()
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "18"
    }
}

dependencies {
    implementation(libs.yahoo.api) {
        isChanging = true
    }
    implementation(libs.bundles.ktor)
    implementation(libs.bundles.hoplite)
    implementation(libs.hikari)
    implementation(libs.postgres)
    implementation(libs.micrometer.prometheus)
    implementation(libs.kotlin.logging)
    implementation(libs.telegram)
    implementation(libs.skedule)
    implementation(libs.krontab)
    implementation(libs.fuzzywuzzy)
    jooqGenerator(libs.postgres)
}

jooq {
    edition.set(nu.studer.gradle.jooq.JooqEdition.OSS) // default (can be omitted)

    configurations {
        create("main") { // name of the jOOQ configuration
            generateSchemaSourceOnCompilation.set(false) // default (can be omitted)

            jooqConfiguration.apply {
                logging = Logging.WARN
                jdbc.apply {
                    driver = "org.postgresql.Driver"
                    url = System.getenv("DB_URL")
                    user = "postgres"
                    password = System.getenv("DB_PASSWORD")
                }
                generator.apply {
                    name = "org.jooq.codegen.DefaultGenerator"
                    database.apply {
                        name = "org.jooq.meta.postgres.PostgresDatabase"
                        inputSchema = "public"
                        forcedTypes.addAll(
                            listOf(
                                ForcedType().apply {
                                    name = "varchar"
                                    includeExpression = ".*"
                                    includeTypes = "JSONB?"
                                },
                                ForcedType().apply {
                                    name = "varchar"
                                    includeExpression = ".*"
                                    includeTypes = "INET"
                                }
                            )
                        )
                    }
                    generate.apply {
                        isDeprecated = false
                        isRecords = true
                        isImmutablePojos = true
                        isFluentSetters = true
                    }
                    target.apply {
                        packageName = "nu.studer.sample"
                        directory = "build/generated-src/jooq/main" // default (can be omitted)
                    }
                    strategy.name = "org.jooq.codegen.DefaultGeneratorStrategy"
                }
            }
        }
    }
}
