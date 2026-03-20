import AuronBuild._
import AuronDeps._

// Print active Spark profile on load
onLoad in Global := {
  val old = (onLoad in Global).value
  s => {
    val sp = SparkVersions.active
    println(s"[info] Auron SBT build — Spark ${sp.shortVersion} (${sp.sparkVersion}), Netty ${sp.nettyVersion}")
    old(s)
  }
}

// ---------------------------------------------------------------------------
// Proto module — compiles .proto files from native-engine/auron-planner/proto
// ---------------------------------------------------------------------------
lazy val proto = (project in file("dev/mvn-build-helper/proto"))
  .settings(commonSettings)
  .settings(
    name := "proto",
    Compile / PB.protoSources := Seq(
      (ThisBuild / baseDirectory).value / "native-engine" / "auron-planner" / "proto"
    ),
    Compile / PB.targets := Seq(
      PB.gens.java(protobufVersion) -> (Compile / sourceManaged).value
    ),
    libraryDependencies += protobufJava
  )

// ---------------------------------------------------------------------------
// Common module
// ---------------------------------------------------------------------------
lazy val common = (project in file("common"))
  .dependsOn(proto)
  .settings(commonSettings)
  .settings(
    name := "auron-common",
    libraryDependencies += sparkCore(scalaBinaryVersion.value) % Provided,
    projectConstantsGenerator(SparkVersions.active.shimName),
    buildInfoResourceGenerator(SparkVersions.active.shimName)
  )

// ---------------------------------------------------------------------------
// Spark Version Annotation Macros
// ---------------------------------------------------------------------------
lazy val sparkVersionAnnotationMacros = (project in file("spark-version-annotation-macros"))
  .dependsOn(common)
  .settings(commonSettings)
  .settings(
    name := "spark-version-annotation-macros",
    // Macro modules need scala-reflect and scala-compiler on compile classpath
    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-reflect" % scalaVersion.value,
      "org.scala-lang" % "scala-compiler" % scalaVersion.value
    )
  )

// ---------------------------------------------------------------------------
// Hadoop Shim
// ---------------------------------------------------------------------------
lazy val hadoopShim = (project in file("hadoop-shim"))
  .settings(commonSettings)
  .settings(
    name := "hadoop-shim",
    libraryDependencies ++= Seq(
      scalaJava8Compat,
      hadoopClientApi % Provided
    )
  )

// ---------------------------------------------------------------------------
// Auron Spark UI
// ---------------------------------------------------------------------------
lazy val auronSparkUi = (project in file("auron-spark-ui"))
  .dependsOn(sparkVersionAnnotationMacros)
  .settings(commonSettings)
  .settings(
    name := "auron-spark-ui",
    libraryDependencies ++= Seq(
      sparkSql(scalaBinaryVersion.value) % Provided,
      scalaXml % Provided
    )
  )

// ---------------------------------------------------------------------------
// Auron Core
// ---------------------------------------------------------------------------
lazy val auronCore = (project in file("auron-core"))
  .dependsOn(proto, hadoopShim)
  .settings(commonSettings)
  .settings(
    name := "auron-core",
    libraryDependencies ++= arrowAll,
    libraryDependencies ++= Seq(
      hadoopClientApi % Provided,
      jsr305,
      junitJupiter % Test
    ),
    // Native library directory for JNI
    Compile / unmanagedJars ++= {
      val nativeDir = (ThisBuild / baseDirectory).value / "native-engine" / "_build"
      if (nativeDir.exists()) (nativeDir ** "*.jar").classpath
      else Nil
    }
  )

// ---------------------------------------------------------------------------
// Spark Extension
// ---------------------------------------------------------------------------
lazy val sparkExtension = (project in file("spark-extension"))
  .dependsOn(auronCore, hadoopShim, sparkVersionAnnotationMacros, proto, auronSparkUi)
  .settings(commonSettings)
  .settings(
    name := "spark-extension",
    libraryDependencies ++= arrowAll,
    libraryDependencies ++= Seq(
      sparkCore(scalaBinaryVersion.value) % Provided,
      sparkSql(scalaBinaryVersion.value) % Provided,
      sparkHive(scalaBinaryVersion.value) % Provided,
      nettyBuffer % Provided,
      nettyCommon % Provided,
      junitJupiter % Test
    )
  )

// ---------------------------------------------------------------------------
// Spark Extension Shims
// ---------------------------------------------------------------------------
lazy val sparkExtensionShims = (project in file("spark-extension-shims-spark"))
  .dependsOn(sparkExtension, common, auronSparkUi)
  .settings(commonSettings)
  .settings(testSettings)
  .settings(
    name := "spark-extension-shims-spark",
    libraryDependencies ++= arrowAll,
    libraryDependencies ++= Seq(
      scalaJava8Compat,
      byteBuddy,
      byteBuddyAgent,
      sparkCore(scalaBinaryVersion.value) % Provided,
      sparkSql(scalaBinaryVersion.value) % Provided,
      sparkHive(scalaBinaryVersion.value) % Provided
    )
  )

// ---------------------------------------------------------------------------
// Assembly (fat JAR)
// ---------------------------------------------------------------------------
lazy val assembly_module = (project in file("dev/mvn-build-helper/assembly"))
  .dependsOn(sparkExtension, sparkExtensionShims)
  .settings(commonSettings)
  .settings(AuronShade.assemblySettings)
  .settings(AuronShade.assemblyExcludes)
  .settings(
    name := s"auron-${SparkVersions.active.shimName}",
    libraryDependencies ++= Seq(
      nettyBuffer,
      nettyCommon
    )
  )

// ---------------------------------------------------------------------------
// Root project — aggregates all modules
// ---------------------------------------------------------------------------
lazy val root = (project in file("."))
  .aggregate(
    proto,
    common,
    sparkVersionAnnotationMacros,
    hadoopShim,
    auronSparkUi,
    auronCore,
    sparkExtension,
    sparkExtensionShims,
    assembly_module
  )
  .settings(commonSettings)
  .settings(
    name := "auron-parent",
    // Root project should not compile anything itself
    Compile / sources := Nil,
    Test / sources := Nil
  )
