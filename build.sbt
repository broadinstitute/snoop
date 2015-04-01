organization  := "org.broadinstitute"

version       := "0.1"

scalaVersion  := "2.11.2"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

libraryDependencies ++= {
  val akkaV = "2.3.6"
  val sprayV = "1.3.2"
  Seq(
    "io.spray"            %%  "spray-can"     % sprayV,
    "io.spray"            %%  "spray-routing" % sprayV,
    "io.spray"            %%  "spray-client"  % sprayV,
    "io.spray"            %%  "spray-http"    % sprayV,
    "io.spray"            %%  "spray-json"    % "1.3.1",
    "com.typesafe.akka"   %%  "akka-actor"    % akkaV,
    "com.typesafe.slick" %% "slick" % "2.1.0",
    "com.typesafe.scala-logging" %% "scala-logging-slf4j" % "2.1.2",
    "c3p0" % "c3p0" % "0.9.1.2",
    "mysql" % "mysql-connector-java" % "5.1.12",
    "org.hsqldb" % "hsqldb" % "2.3.2",
    "com.gettyimages"     %%  "spray-swagger" % "0.5.0",
    "com.google.apis" % "google-api-services-storage" % "v1-rev30-1.20.0",
    "io.spray"            %%  "spray-testkit" % sprayV  % "test",
    "com.typesafe.akka"   %%  "akka-testkit"  % akkaV   % "test",
    "org.scalatest"       %%  "scalatest"     % "2.2.4" % "test",
    "org.liquibase" % "liquibase-core" % "3.3.2" % "test"
  )
}

Revolver.settings

Revolver.enableDebugging(port = 5050, suspend = false)