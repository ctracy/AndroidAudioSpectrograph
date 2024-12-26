/*
 * PROJECT-LEVEL BUILD.GRADLE.KTS
 * =============================
 *
 * PURPOSE:
 * This file acts as the "corporate headquarters" for your Android project's build system.
 * While module-level build files handle specific implementation, this file manages project-wide concerns.
 *
 * MAIN FUNCTIONS:
 * 1. Plugin Version Management
 *    - Declares versions of build tools/plugins for the entire project
 *    - Controls which versions of Gradle, Android tools, and Kotlin are used
 *    - Similar to declaring Maven/Ant versions in traditional Java builds
 *
 * 2. Project-Wide Configuration
 *    - Sets up build rules that apply to ALL modules
 *    - 'apply false' means "make plugin available to modules but don't apply here"
 *
 * 3. Common Settings Repository
 *    - Can define variables and settings shared across all modules
 *    - Similar to parent POM concept in Maven
 *
 * 4. Build System Setup
 *    - Configures the overall build environment
 *    - Like a master build file in Ant, but at a higher level
 *
 * ADDITIONAL CAPABILITIES (not shown in this minimal file):
 * - Define common repositories for all modules
 * - Set up project-wide properties
 * - Configure custom build logic
 * - Define shared dependencies
 *
 * Think of this as the "master control" file for your entire project.
 */

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.2.1" apply false
    id("org.jetbrains.kotlin.android") version "1.9.0" apply false
}

/*
 * POTENTIAL ADDITIONS:
 *
 * // Example of project-wide repository definitions
 * allprojects {
 *     repositories {
 *         google()
 *         mavenCentral()
 *     }
 * }
 *
 * // Example of project-wide properties
 * ext {
 *     kotlinVersion = "1.9.0"
 *     minSdkVersion = 24
 * }
 *
 * // Example of custom build logic
 * tasks.register("clean", Delete::class) {
 *     delete(rootProject.buildDir)
 * }
 */