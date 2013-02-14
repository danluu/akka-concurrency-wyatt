name  :=  "AkkaInvestigation"

version  :=  "0.1"

scalaVersion  :=  "2.10.0"

resolvers  +=  "Typesafe  Repository"  at  "http://repo.typesafe.com/typesafe/releases/"

libraryDependencies  +=
  "com.typesafe.akka"  %%  "akka-actor"  %  "2.1.0"

libraryDependencies  ++=  Seq(
  "org.scalatest"  %%  "scalatest"  %  "1.9-2.10.0-M6-B21"  %  "test",
  "com.typesafe.akka"  %  "akka-testkit"  %  "2.1"
  "com.typesafe.akka"  %  "akka-actor"  %  "2.1"
)
