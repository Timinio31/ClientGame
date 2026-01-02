/*
 * Root-Build für das Multi-Projekt:
 * - shared
 * - server
 * - client
 */

plugins {
    // optional, gibt dir "clean", "assemble" etc. auf Root-Ebene
    `base`
}

allprojects {
    group = "com.tim.clientgame"
    version = "0.0.1-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}
