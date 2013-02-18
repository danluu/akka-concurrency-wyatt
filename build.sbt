name  :=  "AkkaInvestigation"

version := "0.1"

scalaVersion := "2.10.0-RC3"

resolvers  +=  "Typesafe  Repository"  at  "http://repo.typesafe.com/typesafe/releases/"

libraryDependencies ++= Seq(
    "com.typesafe.akka" % "akka-actor"           % "2.1.0-RC3" cross CrossVersion.full,
    "com.typesafe.akka" % "akka-agent"           % "2.1.0-RC3" cross CrossVersion.full,
    "com.typesafe.akka" % "akka-testkit"         % "2.1.0-RC3" cross CrossVersion.full,
    "com.typesafe.akka" % "akka-dataflow"        % "2.1.0-RC3" cross CrossVersion.full,
    "com.typesafe.akka" % "akka-remote"          % "2.1.0-RC3" cross CrossVersion.full,
    "com.typesafe.akka" % "akka-camel"           % "2.1.0-RC3" cross CrossVersion.full,
    "org.scalatest"     % "scalatest_2.10.0-RC3" % "2.0.M5-B1" 
)



