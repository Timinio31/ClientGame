plugins {
    `java-library`
}
java {
    toolchain{
        languageVersion.set(JavaLanguageVersion.of(18))
    }
}
dependencies {
    // später z.B. JSON-Mapper etc.
    // implementation("com.fasterxml.jackson.core:jackson-databind:2.18.0")
}
