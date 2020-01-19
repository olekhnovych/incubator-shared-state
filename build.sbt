import sbt.Keys._
import sbt._

scalaVersion := "2.12.8"

enablePlugins(JavaAppPackaging)

parallelExecution in Test := false
testOptions in Test += Tests.Argument("-oF")   // show full stack traces
fork in Test := false

fork in run := true

resolvers ++= Seq(
  Resolver.mavenLocal,
  Resolver.typesafeRepo("releases"),
  //"Artifactory" at "http://artifactory.service/sbt-release/",
  "confluent.io" at "http://packages.confluent.io/maven/",
)

scalacOptions := Seq("-unchecked",
                     "-deprecation",
                     "-feature",
                     "-encoding", "utf8",
                     "-Ypartial-unification",
                     "-language:higherKinds")

libraryDependencies ++= Dependencies.allDependencies

val heapsize = sys.env.getOrElse("JVM_HEAPSIZE", "8G")

javaOptions in Universal ++= Seq(
  "-J-Xmx"+heapsize,
  "-J-Xms"+heapsize,
  "-J-XX:+UnlockExperimentalVMOptions",
  "-J-XX:+UseShenandoahGC",
  "-J-XX:+UseNUMA",
  "-J-XX:-UseBiasedLocking",
  "-J-XX:+DisableExplicitGC",
  "-J-XX:+AlwaysPreTouch",
  "-J-server",
  "-J-Xlog:gc*:gc.log"
)

name := "shared-state"
organization := "com.synerise.ai"

mainClass in Compile := Some("com.synerise.ai.sharedstate.Boot")
