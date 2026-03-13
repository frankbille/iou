import com.diffplug.spotless.LineEnding

plugins {
    alias(libs.plugins.spotless)
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinJpa) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.springBoot) apply false
    alias(libs.plugins.springDependencyManagement) apply false
}

spotless {
    lineEndings = LineEnding.UNIX

    kotlin {
        target("**/*.kt")
        targetExclude("**/build/**", ".gradle/**", ".context/**", ".serena/**")
        ktlint()
        endWithNewline()
        trimTrailingWhitespace()
    }

    kotlinGradle {
        target("*.gradle.kts", "**/*.gradle.kts")
        targetExclude("**/build/**", ".gradle/**", ".context/**", ".serena/**")
        ktlint()
        endWithNewline()
        trimTrailingWhitespace()
    }

    format("misc") {
        target(
            "*.md",
            "**/*.md",
            "*.properties",
            "**/*.properties",
            "*.yaml",
            "**/*.yaml",
            "*.yml",
            "**/*.yml",
            ".gitattributes",
            ".gitignore",
        )
        targetExclude("**/build/**", ".gradle/**", ".context/**", ".serena/**")
        endWithNewline()
        trimTrailingWhitespace()
    }
}
