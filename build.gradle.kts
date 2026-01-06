import org.jetbrains.changelog.Changelog
import org.jetbrains.grammarkit.tasks.GenerateLexerTask

import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("java")
    id("idea")
    alias(libs.plugins.kotlin) // Kotlin support
    alias(libs.plugins.intelliJPlatform) // IntelliJ Platform Gradle Plugin
    alias(libs.plugins.changelog) // Gradle Changelog Plugin
    alias(libs.plugins.grammarKit)
}

val pluginVersion: String by project
val platformVersion: String by project
val sinceVersion: String by project

group = "glsl.plugin"
version = pluginVersion

kotlin {
    jvmToolchain(21)
}

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
        snapshots()
    }
}


dependencies {
    intellijPlatform {
        intellijIdea(platformVersion) { useInstaller = false }
        testFramework(TestFrameworkType.Platform)
    }
    testImplementation(libs.junit)
    testImplementation(libs.opentest4j)
}

intellijPlatform {
    pluginConfiguration {
        version = pluginVersion
        description = file("plugin-info/description.html").readText()
        changeNotes = changelog.renderItem(changelog.get(pluginVersion), Changelog.OutputType.HTML)
        ideaVersion {
            sinceBuild = "223"
        }
    }
    publishing {
        token = System.getenv("PUBLISH_TOKEN")
    }
    pluginVerification {
        ides {
            select {
                types = listOf(IntelliJPlatformType.IntellijIdeaCommunity)
            }
        }
    }
}

tasks {
    compileJava {
        sourceCompatibility = JavaVersion.VERSION_21.majorVersion
        targetCompatibility = JavaVersion.VERSION_21.majorVersion
    }

    runIde {
        maxHeapSize = "6g"
    }
}

//region grammars
run {
    val grammarGenRoot = layout.buildDirectory.dir("generated/sources/grammarkit")
    val rootPackagePath = "glsl"
    val grammarSources = layout.projectDirectory.dir("grammar")

    val parserDir = grammarGenRoot.map { it.dir("glsl/parser") }
    val lexerDir = grammarGenRoot.map { it.dir("glsl/lexer") }
    val highlightLexerDir = grammarGenRoot.map { it.dir("glsl/highlight") }

    val grammarGenDirs = listOf(parserDir, lexerDir, highlightLexerDir)

    sourceSets {
        main {
            java {
                grammarGenDirs.forEach { srcDir(it) }
            }
        }
    }

    idea {
        module {
            grammarGenDirs.forEach {
                val file = it.get().asFile
                sourceDirs.add(file)
                generatedSourceDirs.add(file)
            }
            sourceDirs.add(grammarSources.asFile)
        }
    }

    tasks {
        generateLexer {
            purgeOldFiles = true
            sourceFile = grammarSources.file("GlslLexer.flex")
            targetOutputDir = lexerDir.map { it.dir(rootPackagePath) }
        }
        val generateHighlightLexer = register<GenerateLexerTask>("generateHighlightLexer") {
            purgeOldFiles = true
            sourceFile = grammarSources.file("GlslHighlightLexer.flex")
            targetOutputDir = highlightLexerDir.map { it.dir(rootPackagePath) }
        }
        generateParser {
            purgeOldFiles = true
            sourceFile = grammarSources.file("GlslGrammar.bnf")
            targetRootOutputDir = parserDir
            pathToParser = "$rootPackagePath/_GlslParser.java"
            pathToPsiRoot = "$rootPackagePath/psi"
        }
        register("generateGrammarClean") {
            dependsOn(generateLexer, generateParser, generateHighlightLexer)
        }
        compileJava {
            dependsOn("generateGrammarClean")
        }
        compileKotlin {
            dependsOn("generateGrammarClean")
        }
    }
}
//endregion