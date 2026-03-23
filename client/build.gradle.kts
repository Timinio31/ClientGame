plugins {
    application
    java
}

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(18))
    }
}

dependencies {
    implementation(project(":shared"))

    // RabbitMQ
    implementation("com.rabbitmq:amqp-client:5.21.0")

    // LibGDX (Desktop)
    implementation("com.badlogicgames.gdx:gdx:1.12.1")
    implementation("com.badlogicgames.gdx:gdx-backend-lwjgl3:1.12.1")
    implementation("com.badlogicgames.gdx:gdx-platform:1.12.1:natives-desktop")

    // Jackson (für JSON Messages/DTOs) – ggf. weglassen, wenn shared das already liefert
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.2")
}

application {
    mainClass.set("com.tim.game.client.ClientDesktopLauncher")
}
