import sbt._

object Dependencies {
  val akkaV                       = "2.6.1"
  val akkaHttpV                   = "10.1.3"
  val akkaHttpCorsV               = "0.3.0"
  val syneriseCommonsV            = "0.3.71"
  val postgresqlV                 = "42.2.5"
  val scalikejdbcV                = "3.3.5"
  val scalikejdbcAsyncV           = "0.12.+"
  val logbackV                    = "1.2.3"
  val scalaLoggingV               = "3.8.0"
  val scalaTestV                  = "3.0.5"
  val jacksonV                    = "2.9.9"
  val jsonpathV                   = "0.7.0"
  val jodaTimeV                   = "2.9.9"
  val testContainersScalaV        = "0.30.0"
  val testcontainersJavaV         = "1.12.0"
  val clientConfigurationGatewayV = "1.0.2"
  val catalogsV                   = "1.0.0"
  val merchantFeedV               = "1.0.1"
  val spireV                      = "0.14.1"
  val catsV                       = "2.0.0"

  lazy val allDependencies: Seq[ModuleID] = Seq(
    "com.typesafe.akka" %% "akka-actor" % akkaV,
    "com.typesafe.akka" %% "akka-actor-typed" % akkaV,
    "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaV,
    "com.typesafe.akka" %% "akka-stream" % akkaV,
    "com.typesafe.akka" %% "akka-slf4j" % akkaV,
    "com.typesafe.akka" %% "akka-testkit" % akkaV,

    "com.typesafe.akka" %% "akka-http" % akkaHttpV,
    "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpV,
    "com.typesafe.akka" %% "akka-http-caching" % akkaHttpV,
    "ch.megard" %% "akka-http-cors" % akkaHttpCorsV,
    "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpV % Test,

    "joda-time" % "joda-time" % jodaTimeV,
    "com.github.pureconfig" %% "pureconfig" % "0.12.2",

    "org.postgresql" % "postgresql" % postgresqlV,
    "org.typelevel" %% "spire" % spireV,
    "org.typelevel" %% "cats-core" % catsV,

    "org.scalikejdbc"       %% "scalikejdbc-async" % scalikejdbcAsyncV,
    "org.scalikejdbc"       %% "scalikejdbc-config" % scalikejdbcV,
    "com.github.jasync-sql" %  "jasync-postgresql" % "1.0.+",


    "ch.qos.logback" % "logback-classic" % logbackV,
    "com.typesafe.scala-logging" %% "scala-logging" % scalaLoggingV,

    // "com.synerise.ai" %% "microservice"         % syneriseCommonsV,
    // "com.synerise.ai" %% "core"                 % syneriseCommonsV,
    // "com.synerise.ai" %% "utils"                % syneriseCommonsV,
    // "com.synerise.ai" %% "filters"              % syneriseCommonsV,
    // "com.synerise.ai" %% "gateways"             % syneriseCommonsV,
    // "com.synerise.ai" %% "configuration-client" % syneriseCommonsV,
    // "com.synerise.ai" %% "notifications-client" % syneriseCommonsV,
    // "com.synerise.ai" %% "reactive-communication" % syneriseCommonsV,

    // "com.synerise.ai.gateway" %% "client-configuration" % clientConfigurationGatewayV,
    // "com.synerise.ai.gateway" %% "catalogs" % catalogsV,
    // "com.synerise.ai.gateway" %% "merchant-feed" % merchantFeedV,

    "com.fasterxml.jackson.core" % "jackson-databind" % jacksonV,
    "io.gatling" %% "jsonpath" % jsonpathV,

    "org.scalatest" %% "scalatest" % scalaTestV % Test,
    "com.dimafeng" %% "testcontainers-scala" % testContainersScalaV % Test,
    "org.testcontainers" % "postgresql" % testcontainersJavaV % Test,
  )
}
