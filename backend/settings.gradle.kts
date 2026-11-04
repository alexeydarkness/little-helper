plugins {
	// Если на компьютере нет JDK 21, Gradle скачает её сам
	id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "backend"
