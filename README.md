# gradle-defaults

Plugin providing opinionated defaults for Gradle builds.

[![Build Status](https://travis-ci.org/jdigger/gradle-defaults.png?branch=master)](https://travis-ci.org/jdigger/gradle-defaults)
[![Maintainer Status](http://stillmaintained.com/jdigger/gradle-defaults.png)](http://stillmaintained.com/jdigger/gradle-defaults)
[ ![Download](https://api.bintray.com/packages/jmoore/java-lib/com.mooregreatsoftware%3Agradle-defaults/images/download.svg) ](https://bintray.com/jmoore/java-lib/com.mooregreatsoftware%3Agradle-defaults/_latestVersion)
[![Stories in Ready](https://badge.waffle.io/jdigger/gradle-defaults.png?label=ready&title=Ready)](https://waffle.io/jdigger/gradle-defaults)

## Core Functionality

Currently, the defaults aren't terribly easy to override or pick and choose from. Please submit an issue if you find anything you would like to be more configurable.

- Applies [`organize-imports`](https://github.com/ajoberstar/gradle-imports) plugin.
- Adds the `jcenter()` repository.
- Applies [`org.ajoberstar.github-pages`](https://github.com/ajoberstar/gradle-git) plugin. Configures to use:
    - Extension's `vcsWriteUrl`.
    - Publish content from `src/gh-pages`.
- If `java` plugin applied:
    - Apply `idea` plugin.
    - Configures the `idea` plugin:
        - Git VCS
        - JDK 1.8
        - Sets the language level to `compatibilityVersion`
        - Code formatting set to reasonable/consistent standards
    - Configure `org.ajoberstar.github-pages` to deploy `docs/javadoc`.
    - Add tasks and artifacts for a `-sources.jar` and `-javadoc.jar`.
    - Sets the compiler target and source compatibility versions based on the `defaults.compatibilityVersion` (defaults to 1.7)
    - Configures the JAR MANIFEST.MF to include
        - Implementation-Title = {project.description ?: project.name}
        - Implementation-Version = {project.version}
        - Built-By = {userEmail as known to git's `user.email` configuration}
        - Built-Date = {now}
        - Built-JDK = {System.getProperty('java.version') ?: '1.7'}
        - Built-Gradle = {Gradle version being used}
- If `groovy` plugin applied:
    - Configure `org.ajoberstar.github-pages` to deploy `docs/groovydoc`
    - Adds tasks and artifacts for `-groovydoc.jar`.
    - Sets the compiler target and source compatibility versions based on the `defaults.compatibilityVersion` (defaults to 1.7)
- If `scala` plugin applied:
    - Configure `org.ajoberstar.github-pages` to deploy `docs/scaladoc`.
    - Add tasks and artifacts for `-scaladoc.jar`.
    - Sets the compiler target and source compatibility versions based on the `defaults.compatibilityVersion` (defaults to 1.7)
- Applies [`license`](https://github.com/hierynomus/license-gradle-plugin) plugin.
    - Uses license header from `gradle/HEADER`
        - Substitutes extensions' `copyrightYears` for `${year}` in header.
    - Enforces strict checking.
    - Disables normal mappings and sets `groovy` and `java` files to use slashstar style instead of default javadoc style.
    - Excludes properties files from checks.
- Applies [`org.ajoberstar.release-opinion`](https://github.com/ajoberstar/gradle-git) plugin.
    - Configures to use the project's root dir as the repository.
    - Configures the `release` task to depend on `clean`, `build` and `publishGhPages`.
    - If `com.jfrog.bintray` applied:
        - Configure the `release` task to depend on `bintrayUpload`.
- Configures ordering rules for tasks:
    - All tasks should run after `clean`.
    - All tasks in the `publishing` group should run after `build`.
- Applies the `maven-publish` plugin:
    - Configure the `main` publication to include any of the following that exist:
        - `.jar`
        - `-sources.jar`
        - `-javadoc.jar`
        - `-groovydoc.jar`
        - `-scaladoc.jar`
    - Configures the POM with information from project and extension.
- If [`com.jfrog.bintray`](https://github.com/bintray/gradle-bintray-plugin) applied and `bintrayUser` and `bintrayKey` properties are set on the project:
    - If the extension's `bintrayLabels` includes `gradle`:
        - Set the `gradle-plugin` attribute to support the Gradle plugin portal for any plugin IDs in `src/main/resources`.
    - Publishes the `main` publication.
    - Configures settings using the project and extension.

## Configuration

```groovy
buildscript {
    repositories {
        maven {
            url "http://dl.bintray.com/jmoore/java-lib"
        }
    }

    dependencies {
        classpath 'com.mooregreatsoftware:gradle-defaults:<version>'
    }
}

apply plugin: 'java'
apply plugin: 'maven-publish'
apply plugin: 'com.mooregreatsoftware.defaults'

plugins {
    id 'com.jfrog.bintray' version '<version>'
}

group = 'my.group'
description = 'description of project'

defaults {
    id = 'github and bintray user or org ID'

    // if using an organization in github and bintray
    orgName = 'friendly name of org'
    orgUrl = 'website of org'

    compatibilityVersion = 1.7

    bintrayRepo = 'my repo'
    bintrayLabels = ['label1', 'label2']

    // optional
    developers = [
        [id: 'github id', name: 'your name', email: 'your email']
    ]

    // optional
    contributors = [
        [id: 'github id', name: 'their name', email: 'their email']
    ]

    // used by license plugin
    copyrightYears = '2013-2014'
}
```

## Release Notes

### v1.0.0

- First release after forking from https://github.com/ajoberstar/gradle-defaults

### v0.5.0

- Automatically configures the Bintray plugins `mavenCentralSync` with the
following project properties:
    - `sonatypeUsername`
    - `sonatypePassword`

### v0.4.0

- Initial release after refactoring from `gradle-ajoberstar`.
