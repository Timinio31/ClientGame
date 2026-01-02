plugins {
    application
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(18))
    }
}

dependencies {
    implementation(project(":shared"))
    implementation("com.rabbitmq:amqp-client:5.21.0")
    // LibGDX kommt später dazu
}

application {
    mainClass.set("com.tim.game.client.GameClientMain")
}
