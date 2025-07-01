import org.jetbrains.changelog.Changelog
import org.jetbrains.grammarkit.tasks.GenerateLexerTask
import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    kotlin("jvm") version "2.1.0"
    id("org.jetbrains.intellij.platform") version "2.6.0"
    id("org.jetbrains.grammarkit") version "2022.3.2.2"
    id("org.jetbrains.changelog") version "2.2.1"
    java
    idea
}

val pluginVersion: String by project

group = "glsl.plugin"
version = pluginVersion

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}


dependencies {
    intellijPlatform {
        intellijIdeaCommunity("2025.1")
        testFramework(TestFrameworkType.Platform)
    }
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.opentest4j:opentest4j:1.3.0")
}

tasks {
    compileJava {
        sourceCompatibility = JavaVersion.VERSION_21.majorVersion
        targetCompatibility = JavaVersion.VERSION_21.majorVersion
    }

    runIde {
        maxHeapSize = "6g"
    }

    patchPluginXml {
        val descriptionHtml = file("plugin-info/description.html").readText()
        changeNotes = changelog.renderItem(changelog.get(version.toString()), Changelog.OutputType.HTML)
        pluginDescription = descriptionHtml
        sinceBuild = "223"
    }

    changelog {
        version = pluginVersion
    }

    publishPlugin {
        token = System.getenv("PUBLISH_TOKEN")
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