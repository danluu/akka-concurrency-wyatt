import sbt._
import Keys._
import com.typesafe.sbt.SbtMultiJvm
import com.typesafe.sbt.SbtMultiJvm.MultiJvmKeys.{MultiJvm}
object AkkaBookBuild extends Build {
  val ScalaVersion = "2.10.0"
  val AkkaVersion = "2.1.1"
  val ScalaTestVersion = "2.0.M5b"
  val JUnitVersion = "4.10"
  lazy val buildSettings = Defaults.defaultSettings ++
  multiJvmSettings ++ Seq(
    organization := "zzz.akka",
    version := "0.1",
    scalaVersion := ScalaVersion,
    autoCompilerPlugins := true,
    resolvers ++= Seq(
      "Typesafe Repository" at
        "http://repo.typesafe.com/typesafe/releases/",
      "Sonatype" at
        "https://oss.sonatype.org/content/groups/public/"
    ) )
  lazy val multiJvmSettings = SbtMultiJvm.multiJvmSettings ++
  Seq(
    // make sure that MultiJvm test are compiled by the
    // default test compilation
    compile in MultiJvm <<=
      (compile in MultiJvm) triggeredBy (compile in Test),
    // disable parallel tests
    parallelExecution in Test := false,
    // make sure that MultiJvm tests are executed by the
    // default test target
    executeTests in Test <<=
      ((executeTests in Test), (executeTests in MultiJvm)) map {
        case ((_, testResults), (_, multiJvmResults))  =>
          val results = testResults ++ multiJvmResults
          (Tests.overall(results.values), results)
      } )
  lazy val avionics = Project(
    id = "avionics",
    base = file("."),
    settings = buildSettings ++ Seq(libraryDependencies ++=
      Dependencies.avionics)
  ) configs(MultiJvm)
  object Dependencies {
    val avionics = Seq(
      "com.typesafe.akka" %% "akka-actor" % AkkaVersion,
      "com.typesafe.akka" %% "akka-testkit" % AkkaVersion,
      "com.typesafe.akka" %% "akka-remote" % AkkaVersion,
      "junit"              % "junit"       % JUnitVersion,
      "org.scalatest"     %% "scalatest"   % ScalaTestVersion

    ) }
}
