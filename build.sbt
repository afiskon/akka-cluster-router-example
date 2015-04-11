name := "akka-cluster-router-example"

version := "0.1"

scalaVersion := "2.11.4"

val akkaVersion = "2.4-SNAPSHOT"

libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-actor" % akkaVersion,
    "com.typesafe.akka" %% "akka-contrib" % akkaVersion,
    "com.typesafe.akka" %% "akka-cluster" % akkaVersion
  )

resolvers += "Akka Snapshots" at "http://repo.akka.io/snapshots/"
