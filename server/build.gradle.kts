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
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.2")
}

application {
    mainClass.set("com.tim.game.server.GameServerMain")
}
