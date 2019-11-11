name := """lab3"""

version := "1.0"

scalaVersion := "2.13.1"

val akkaVersion = "2.6.0"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,
  "org.scalatest" %% "scalatest" % "3.0.8" % "test")

// scalaFmt
scalafmtOnCompile := true
