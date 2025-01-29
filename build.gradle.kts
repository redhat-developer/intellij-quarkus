import com.adarshr.gradle.testlogger.theme.ThemeType
import org.jetbrains.changelog.markdownToHTML
import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.tasks.VerifyPluginTask.FailureLevel.*
import org.jetbrains.intellij.platform.gradle.tasks.VerifyPluginTask.VerificationReportsFormats.*
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.models.ProductRelease
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.regex.Pattern

fun properties(key: String) = providers.gradleProperty(key)
fun environment(key: String) = providers.environmentVariable(key)
fun prop(name: String): String {
    return properties(name).get()
}
plugins {
    java // Java support
    alias(libs.plugins.kotlin) // Kotlin support
    alias(libs.plugins.gradleIntelliJPlugin) // Gradle IntelliJ Plugin
    alias(libs.plugins.changelog) // Gradle Changelog Plugin
    alias(libs.plugins.testLogger) // Nice test logs
    jacoco // Code coverage
}

group = prop("pluginGroup")
version = prop("pluginVersion")

val quarkusVersion = prop("quarkusVersion")
val lsp4mpVersion = prop("lsp4mpVersion")
val quarkusLsVersion = prop("quarkusLsVersion")
val quteLsVersion = prop("quteLsVersion")
// Configure project's dependencies
repositories {
    mavenLocal()
    mavenCentral()
    maven { url = uri("https://repository.jboss.org/nexus/content/repositories/snapshots") }
    maven { url = uri("https://repository.jboss.org/nexus/content/groups/public") }
    maven { url = uri("https://repo.eclipse.org/content/repositories/lsp4mp-snapshots") }
    maven { url = uri("https://repo.eclipse.org/content/repositories/lsp4mp-releases") }
    intellijPlatform {
        defaultRepositories()
    }
}

val lsp: Configuration by configurations.creating

sourceSets {
    named("test") {
        java.srcDir("src/test/java")
        java.srcDir("intellij-community/java/testFramework/src")
        java.srcDir("intellij-community/platform/external-system-api/testFramework/src")
        java.srcDir("intellij-community/platform/external-system-impl/testSrc")
        java.srcDir("intellij-community/platform/lang-impl/testSources")
        java.srcDir("intellij-community/platform/testFramework/extensions/src")
        java.srcDir("intellij-community/plugins/gradle/src")
        java.srcDir("intellij-community/plugins/gradle/testSources")
        java.srcDir("intellij-community/plugins/gradle/tooling-extension-impl/testSources")
        java.srcDir("intellij-community/plugins/maven/src/test/java")
        java.srcDir("intellij-community/plugins/maven/testFramework/src")
        resources.srcDir("src/test/resources")
    }

    create("integrationTest") {
        java.srcDir("src/it/java")
        resources.srcDir("src/it/resources")
        compileClasspath += sourceSets.main.get().compileClasspath + sourceSets.test.get().compileClasspath
        runtimeClasspath += output + compileClasspath + sourceSets.test.get().runtimeClasspath + sourceSets.test.get().runtimeClasspath
    }
}

// Dependencies are managed with Gradle version catalog - read more: https://docs.gradle.org/current/userguide/platforms.html#sub:version-catalog

dependencies {
    intellijPlatform {
        create(prop("platformType"), prop("platformVersion"))
        // Bundled Plugin Dependencies. Uses `platformBundledPlugins` property from the gradle.properties file for bundled IntelliJ Platform plugins.
        val platformBundledPlugins =  ArrayList<String>()
        platformBundledPlugins.addAll(providers.gradleProperty("platformBundledPlugins").map { it.split(',').map(String::trim).filter(String::isNotEmpty) }.get())
        bundledPlugins(platformBundledPlugins)
        // Plugin Dependencies. Uses `platformPlugins` property from the gradle.properties file for plugin from JetBrains Marketplace.
        val platformPlugins =  ArrayList<String>()
        val localLsp4ij = file("../lsp4ij/build/idea-sandbox/plugins/LSP4IJ").absoluteFile
        if (localLsp4ij.isDirectory) {
            // In case Gradle fails to build because it can't find some missing jar, try deleting
            // ~/.gradle/caches/modules-2/files-2.1/com.jetbrains.intellij.idea/unzipped.com.jetbrains.plugins/com.redhat.devtools.lsp4ij*
            platformPlugins.add(localLsp4ij.toString())
        } else {
            // When running on CI or when there's no local lsp4ij
            val latestLsp4ijNightlyVersion = fetchLatestLsp4ijNightlyVersion()
            platformPlugins.add("com.redhat.devtools.lsp4ij:$latestLsp4ijNightlyVersion@nightly")
        }
        //Uses `platformPlugins` property from the gradle.properties file.
        platformPlugins.addAll(properties("platformPlugins").map { it.split(',').map(String::trim).filter(String::isNotEmpty) }.get())
        plugins(platformPlugins)
        // for local plugin -> https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin-faq.html#how-to-add-a-dependency-on-a-plugin-available-in-the-file-system
        //plugins.set(listOf(file("/path/to/plugin/")))
        // Print plugins
        println("bundledPlugins: $platformBundledPlugins")
        println("marketplacePlugins: $platformPlugins")
        pluginVerifier()
        instrumentationTools()
        testFramework(TestFrameworkType.Plugin.Java)
    }

    implementation("org.zeroturnaround:zt-zip:1.14")
    implementation("org.jsoup:jsoup:1.17.1")
    implementation("com.vladsch.flexmark:flexmark-html2md-converter:0.64.8") {
        exclude(group="com.vladsch.flexmark", module= "flexmark-jira-converter")
    }
    implementation("com.google.code.gson:gson:2.10.1") //Need to ensure we don't get telemetry's old gson version
    implementation("io.quarkus:quarkus-core:$quarkusVersion") {
        isTransitive = false
    }
    implementation("io.quarkus:quarkus-core-deployment:$quarkusVersion") {
        exclude(group = "org.aesh")
        exclude(group = "org.apache.commons")
        exclude(group = "org.wildfly.common")
        exclude(group = "io.quarkus.gizmo")
        exclude(group = "org.ow2.asm")
        exclude(group = "io.quarkus")
        exclude(group = "org.eclipse.sisu")
        exclude(group = "org.graalvm.sdk")
        exclude(group = "org.junit.platform")
        exclude(group = "org.junit.jupiter")
    }
    implementation("io.quarkus:quarkus-arc:$quarkusVersion") {
        isTransitive = false
    }

    implementation("org.eclipse.lsp4mp:org.eclipse.lsp4mp.ls:$lsp4mpVersion") {
        exclude("org.eclipse.lsp4j")
    }
    // Exclude all lsp4j dependencies to use LSP4J from LSP4IJ
    implementation("com.redhat.microprofile:com.redhat.qute.ls:$quteLsVersion") {
        exclude("org.eclipse.lsp4j")
    }
    lsp("org.eclipse.lsp4mp:org.eclipse.lsp4mp.ls:$lsp4mpVersion:uber") {
        isTransitive = false
    }
    lsp("com.redhat.microprofile:com.redhat.quarkus.ls:$quarkusLsVersion") {
        isTransitive = false
    }
    lsp("com.redhat.microprofile:com.redhat.qute.ls:$quteLsVersion:uber") {
        isTransitive = false
    }
    implementation(files(layout.buildDirectory.dir("server")) {
        builtBy("copyDeps")
    })

    implementation(libs.annotations) // to build against platform <= 2023.2 and gradle intellij plugin > 2.0

    // And now for some serious HACK!!!
    // Starting with 2023.1, all gradle tests fail importing projects with a:
    // com.intellij.openapi.externalSystem.model.ExternalSystemException: Unable to load class 'org.codehaus.plexus.logging.Logger'
    // Hence adding a jar containing the missing class, to the test classpath
    // The version matches the jar found in the IJ version used to compile the project
    // This is so wrong/ridiculous!
    testImplementation("org.eclipse.sisu:org.eclipse.sisu.plexus:0.3.4")

    testImplementation("org.assertj:assertj-core:3.19.0")

    testImplementation("org.opentest4j:opentest4j:1.3.0")

    testImplementation("junit:junit:4.13.2")
}

// Set the JVM language level used to build the project. Use Java 11 for 2020.3+, and Java 17 for 2022.2+.
kotlin {
    jvmToolchain {
        languageVersion = JavaLanguageVersion.of(17)
        vendor = JvmVendorSpec.JETBRAINS
    }
}

// Configure Gradle IntelliJ Plugin - read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            untilBuild = provider { null }
        }
    }

    pluginVerification {
        failureLevel = listOf(INVALID_PLUGIN, COMPATIBILITY_PROBLEMS, MISSING_DEPENDENCIES )
        verificationReportsFormats = listOf(MARKDOWN, HTML)
        ides {
            recommended()
        }
        freeArgs = listOf(
            "-mute",
            "TemplateWordInPluginId"
        )
    }
}

configurations {
    runtimeClasspath {
        exclude(group = "org.slf4j", module = "slf4j-api")
    }
}

testlogger {
    theme = ThemeType.STANDARD
    showExceptions = true
    showStackTraces = true
    showFullStackTraces = false
    showCauses = true
    slowThreshold = 2000
    showSummary = true
    showSimpleNames = false
    showPassed = true
    showSkipped = true
    showFailed = true
    showOnlySlow = false
    showStandardStreams = false
    showPassedStandardStreams = true
    showSkippedStandardStreams = true
    showFailedStandardStreams = true
    logLevel = LogLevel.LIFECYCLE
}

// Configure Gradle Changelog Plugin - read more: https://github.com/JetBrains/gradle-changelog-plugin
changelog {
    groups.empty()
    repositoryUrl = properties("pluginRepositoryUrl")
}

tasks.withType<Test> {
    environment("GRADLE_RELEASE_REPOSITORY","https://services.gradle.org/distributions")
    systemProperty("idea.log.leaked.projects.in.tests", "false")
    systemProperty( "idea.maven.test.mirror", "https://repo1.maven.org/maven2")
    systemProperty( "com.redhat.devtools.intellij.telemetry.mode", "disabled")
    systemProperty("java.awt.headless","true")
}

tasks.withType<Copy> {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

val integrationTest by intellijPlatformTesting.testIde.registering {

    task {
        useJUnitPlatform()
        description = "Runs the integration tests."
        group = "verification"
        testClassesDirs = sourceSets["integrationTest"].output.classesDirs
        classpath = sourceSets["integrationTest"].runtimeClasspath
        outputs.upToDateWhen { false }
        mustRunAfter(tasks["test"])
        systemProperty ("debug-retrofit", "enable")
    }
    plugins {
        robotServerPlugin("0.11.23")
    }

    dependencies {
        testImplementation("com.redhat.devtools.intellij:intellij-common-ui-test-library:0.4.3")
        testImplementation("com.squareup.retrofit2:retrofit:2.11.0")
        testImplementation("com.squareup.retrofit2:converter-gson:2.11.0")
        testImplementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    }
}

tasks.register<Copy>("copyDeps") {
    val serverDir = layout.buildDirectory.dir("server/server")
    from(lsp)
    into(serverDir)
    rename("^(.*)(-[0-9]+.[0-9]+.[0-9]+(-SNAPSHOT)?)(.*)$", "$1$4")
    doLast {
        val destinationDir = serverDir.get().asFile
        val numFilesCopied = destinationDir.listFiles()?.size ?: 0
        logger.quiet("Copied $numFilesCopied JARs from lsp configuration to ${destinationDir}.")
    }
}


tasks {
    wrapper {
        gradleVersion = properties("gradleVersion").get()
    }

    patchPluginXml {
        sinceBuild = properties("pluginSinceBuild")
        untilBuild = properties("pluginUntilBuild")

        // Extract the <!-- Plugin description --> section from README.md and provide for the plugin's manifest
        pluginDescription = providers.fileContents(layout.projectDirectory.file("README.md")).asText.map {
            val start = "<!-- Plugin description -->"
            val end = "<!-- Plugin description end -->"

            with(it.lines()) {
                if (!containsAll(listOf(start, end))) {
                    throw GradleException("Plugin description section not found in README.md:\n$start ... $end")
                }
                subList(indexOf(start) + 1, indexOf(end)).joinToString("\n").let(::markdownToHTML)
            }
        }

        //TODO inject changelog into plugin.xml change-notes
    }

    runIde {
        systemProperties["com.redhat.devtools.intellij.telemetry.mode"] = "disabled"
    }

    prepareSandbox {
        dependsOn("copyDeps")
    }

    jacocoTestReport {
        reports {
            xml.required = true
            html.required = true
        }
    }

    signPlugin {
        certificateChain = environment("CERTIFICATE_CHAIN")
        privateKey = environment("PRIVATE_KEY")
        password = environment("PRIVATE_KEY_PASSWORD")
    }

    publishPlugin {
        //dependsOn("patchChangelog") // TODO generate changelog
        token = environment("PUBLISH_TOKEN")
        channels = properties("channel").map { listOf(it) }
    }

    check {
        dependsOn(jacocoTestReport)
    }

    printProductsReleases {
        channels = listOf(ProductRelease.Channel.EAP)
        types = listOf(IntelliJPlatformType.IntellijIdeaCommunity)
        untilBuild = provider { null }
    }
}

// https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-tasks.html#runIdeForUiTests
val runIdeForUiTests by intellijPlatformTesting.runIde.registering {
    task {
        systemProperty ("debug-retrofit", "enable")
        jvmArgumentProviders += CommandLineArgumentProvider {
            listOf(
                "-Drobot-server.port=8580",
                "-Dide.mac.message.dialogs.as.sheets=false",
                "-Djb.privacy.policy.text=<!--999.999-->",
                "-Djb.consents.confirmation.enabled=false",
            )
        }
    }
    plugins {
        robotServerPlugin("0.11.23")
    }
}

fun fetchLatestLsp4ijNightlyVersion(): String {
    val client = HttpClient.newBuilder().build()
    var onlineVersion = ""
    try {
        val request: HttpRequest = HttpRequest.newBuilder()
            .uri(URI("https://plugins.jetbrains.com/api/plugins/23257/updates?channel=nightly&size=1"))
            .GET()
            .timeout(Duration.of(10, ChronoUnit.SECONDS))
            .build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        val pattern = Pattern.compile("\"version\":\"([^\"]+)\"")
        val matcher = pattern.matcher(response.body())
        if (matcher.find()) {
            onlineVersion = matcher.group(1)
            println("Latest approved nightly build: $onlineVersion")
        }
    } catch (e:Exception) {
        println("Failed to fetch LSP4IJ nightly build version: ${e.message}")
    }

    val minVersion = "0.0.1-20231213-012910"
    return if (minVersion < onlineVersion) onlineVersion else minVersion
}
