plugins {
  java
  application
  id("com.google.cloud.tools.jib") version "3.4.4"
}

repositories {
  mavenCentral()
}

val vertxVersion = "5.0.0.CR2"
val verticle = "io.vertx.howtos.clientsidelb.MicroServiceVerticle"

dependencies {
  implementation("io.vertx:vertx-core:${vertxVersion}")
  implementation("io.vertx:vertx-service-resolver:${vertxVersion}")
  implementation("io.vertx:vertx-launcher-application:${vertxVersion}")
}

jib {
  to {
    image = "client-side-lb/microservice"
  }
  container {
    mainClass = "io.vertx.launcher.application.VertxApplication"
    args = listOf(verticle)
    ports = listOf("8080")
  }
}

tasks.wrapper {
  gradleVersion = "8.11.1"
}
