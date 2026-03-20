import sbt._
import sbt.Keys._
import org.scalafmt.sbt.ScalafmtPlugin.autoImport._

object AuronBuild {

  val auronVersion = "7.0.0-SNAPSHOT"

  val commonSettings: Seq[Setting[_]] = Seq(
    organization := "org.apache.auron",
    version := auronVersion,
    scalaVersion := "2.12.18",
    crossScalaVersions := Seq("2.12.18", "2.13.17"),
    javacOptions ++= Seq("-source", "8", "-target", "8"),
    scalacOptions ++= {
      val base = Seq(
        "-deprecation",
        "-feature",
        "-Ywarn-unused",
        "-Xfatal-warnings",
        "-Wconf:msg=method newInstance in class Class is deprecated:s",
        "-Wconf:msg=class ThreadDeath in package lang is deprecated:s"
      )
      val targetFlag = scalaBinaryVersion.value match {
        case "2.12" => Seq("-target:jvm-1.8")
        case _      => Seq("-target:8")
      }
      base ++ targetFlag
    },
    scalacOptions ++= {
      if (scalaBinaryVersion.value == "2.13")
        Seq(
          "-Ymacro-annotations",
          "-Wconf:cat=deprecation:wv,any:e",
          "-Wconf:cat=other-nullary-override:s",
          "-Wconf:msg=^(?=.*?method|value|type|object|trait|inheritance)(?=.*?deprecated)(?=.*?since 2.13).+$:s",
          "-Wconf:msg=Auto-application to `\\(\\)` is deprecated:s",
          "-Wconf:msg=object JavaConverters in package collection is deprecated:s",
          "-Wconf:cat=unchecked&msg=outer reference:s",
          "-Wconf:cat=unchecked&msg=eliminated by erasure:s",
          "-Wconf:cat=unused-nowarn:s",
          "-Wconf:msg=early initializers are deprecated:s",
          "-Wconf:cat=other-match-analysis:s",
          "-Wconf:cat=feature-existentials:s"
        )
      else Nil
    },
    // Paradise compiler plugin for Scala 2.12
    libraryDependencies ++= {
      if (scalaBinaryVersion.value == "2.12")
        Seq(
          compilerPlugin(
            "org.scalamacros" % "paradise" % AuronDeps.paradiseVersion cross CrossVersion.full
          )
        )
      else Nil
    },
    // SemanticDB compiler plugin
    libraryDependencies ++= {
      Seq(
        compilerPlugin(
          "org.scalameta" % "semanticdb-scalac" % AuronDeps.semanticdbVersion cross CrossVersion.full
        )
      )
    },
    resolvers ++= Seq(
      "GCS Maven Central mirror" at "https://maven-central-asia.storage-download.googleapis.com/maven2/",
      "Maven Central" at "https://repo.maven.apache.org/maven2"
    ),
    // Point scalafmt to project's scalafmt.conf (not the default .scalafmt.conf)
    scalafmtConfig := (ThisBuild / baseDirectory).value / "scalafmt.conf",
    // Common test deps inherited from parent POM
    libraryDependencies += AuronDeps.scalaTest % Test,
    // Disable publishing for local dev build
    publish / skip := true,
    publishLocal / skip := true
  )

  val extraJavaTestArgs: Seq[String] = Seq(
    "-XX:+IgnoreUnrecognizedVMOptions",
    "--add-opens=java.base/java.lang=ALL-UNNAMED",
    "--add-opens=java.base/java.lang.invoke=ALL-UNNAMED",
    "--add-opens=java.base/java.lang.reflect=ALL-UNNAMED",
    "--add-opens=java.base/java.io=ALL-UNNAMED",
    "--add-opens=java.base/java.net=ALL-UNNAMED",
    "--add-opens=java.base/java.nio=ALL-UNNAMED",
    "--add-opens=java.base/java.util=ALL-UNNAMED",
    "--add-opens=java.base/java.util.concurrent=ALL-UNNAMED",
    "--add-opens=java.base/java.util.concurrent.atomic=ALL-UNNAMED",
    "--add-opens=java.base/jdk.internal.ref=ALL-UNNAMED",
    "--add-opens=java.base/sun.nio.ch=ALL-UNNAMED",
    "--add-opens=java.base/sun.nio.cs=ALL-UNNAMED",
    "--add-opens=java.base/sun.security.action=ALL-UNNAMED",
    "--add-opens=java.base/sun.security.tools.keytool=ALL-UNNAMED",
    "--add-opens=java.base/sun.security.x509=ALL-UNNAMED",
    "--add-opens=java.base/sun.util.calendar=ALL-UNNAMED",
    "-Djdk.reflect.useDirectMethodHandle=false",
    "-Dio.netty.tryReflectionSetAccessible=true"
  )

  val testSettings: Seq[Setting[_]] = Seq(
    Test / fork := true,
    Test / javaOptions ++= extraJavaTestArgs,
    Test / javaOptions ++= Seq(
      "-Xmx1g",
      s"-Djava.io.tmpdir=${(Test / target).value}/tmp",
      "-Dspark.driver.memory=1g"
    )
  )

  /** Source generator that produces ProjectConstants.java from the template. */
  def projectConstantsGenerator(shimName: String): Setting[_] =
    Compile / sourceGenerators += Def.task {
      val templateFile = baseDirectory.value / "src" / "main" / "templates" /
        "org" / "apache" / "auron" / "common" / "ProjectConstants.java"
      val outDir = (Compile / sourceManaged).value / "org" / "apache" / "auron" / "common"
      outDir.mkdirs()
      val outFile = outDir / "ProjectConstants.java"
      val template = IO.read(templateFile)
      val rendered = template
        .replace("${project.version}", auronVersion)
        .replace("${shimName}", shimName)
      IO.write(outFile, rendered)
      Seq(outFile)
    }.taskValue

  /** Resource generator that produces auron-build-info.properties. */
  def buildInfoResourceGenerator(shimName: String): Setting[_] =
    Compile / resourceGenerators += Def.task {
      val outDir = (Compile / resourceManaged).value
      outDir.mkdirs()
      val outFile = outDir / "auron-build-info.properties"
      val props = Seq(
        s"project.version=${auronVersion}",
        s"spark.version=${SparkVersions.active.shortVersion}",
        s"scala.version=${scalaBinaryVersion.value}",
        s"build.timestamp=${java.time.Instant.now()}"
      ).mkString("\n")
      IO.write(outFile, props)
      Seq(outFile)
    }.taskValue
}
