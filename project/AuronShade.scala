import sbt._
import sbt.Keys._
import sbtassembly.AssemblyPlugin.autoImport._
import sbtassembly.{MergeStrategy, PathList}

object AuronShade {

  val shadePrefix = "auron"

  val assemblySettings: Seq[Setting[_]] = Seq(
    assembly / assemblyJarName := {
      val shimName = SparkVersions.active.shimName
      val sv = scalaBinaryVersion.value
      val v = version.value
      s"auron-${shimName}_${sv}-${v}.jar"
    },
    assembly / assemblyShadeRules := {
      // Arrow JARs to shade (everything except arrow-c-data)
      val arrowLibs = Seq(
        "org.apache.arrow" % "arrow-compression" % AuronDeps.arrowVersion,
        "org.apache.arrow" % "arrow-format" % AuronDeps.arrowVersion,
        "org.apache.arrow" % "arrow-memory-core" % AuronDeps.arrowVersion,
        "org.apache.arrow" % "arrow-memory-unsafe" % AuronDeps.arrowVersion,
        "org.apache.arrow" % "arrow-vector" % AuronDeps.arrowVersion
      )
      Seq(
        ShadeRule.rename("com.google.flatbuffers.**" -> s"$shadePrefix.com.google.flatbuffers.@1").inAll,
        ShadeRule.rename("com.google.protobuf.**" -> s"$shadePrefix.com.google.protobuf.@1").inAll,
        // Shade arrow classes only from non-c-data JARs; also shade references in project classes
        ShadeRule
          .rename("org.apache.arrow.**" -> s"$shadePrefix.org.apache.arrow.@1")
          .inLibrary(arrowLibs: _*)
          .inProject,
        ShadeRule.rename("io.netty.**" -> s"$shadePrefix.io.netty.@1").inAll,
        ShadeRule.rename("javax.annotation.**" -> s"$shadePrefix.javax.annotation.@1").inAll,
        ShadeRule.rename("net.bytebuddy.**" -> s"$shadePrefix.net.bytebuddy.@1").inAll,
        ShadeRule.rename("org.checkerframework.**" -> s"$shadePrefix.org.checkerframework.@1").inAll,
        ShadeRule.rename("scala.compat.**" -> s"$shadePrefix.scala.compat.@1").inAll
      )
    },
    // Exclude arrow-c-data classes from shading (must NOT be relocated)
    assembly / assemblyExcludedJars := {
      val cp = (assembly / fullClasspath).value
      // Keep arrow-c-data on the classpath but exclude from shading via merge strategy
      Nil
    },
    assembly / assemblyOption := (assembly / assemblyOption).value
      .withIncludeScala(false)
      .withIncludeDependency(true),
    assembly / assemblyMergeStrategy := {
      case PathList("META-INF", xs @ _*) if xs.lastOption.exists(n =>
            n.endsWith(".SF") || n.endsWith(".DSA") || n.endsWith(".RSA")) =>
        MergeStrategy.discard
      case PathList("META-INF", xs @ _*) if xs.lastOption.contains("module-info.class") =>
        MergeStrategy.discard
      case x if x.endsWith(".semanticdb") =>
        MergeStrategy.discard
      case x if x.endsWith("module-info.class") =>
        MergeStrategy.discard
      // arrow-c-data classes must not be shaded
      case x if x.startsWith("org/apache/arrow/c/") =>
        MergeStrategy.first
      // Duplicate properties files from Arrow/Netty modules
      case "arrow-git.properties" =>
        MergeStrategy.first
      case PathList("META-INF", "io.netty.versions.properties") =>
        MergeStrategy.first
      case PathList("META-INF", "native-image", _*) =>
        MergeStrategy.first
      case PathList("META-INF", "MANIFEST.MF") =>
        MergeStrategy.discard
      case PathList("META-INF", "LICENSE") | PathList("META-INF", "LICENSE.txt") =>
        MergeStrategy.first
      case PathList("META-INF", "NOTICE") | PathList("META-INF", "NOTICE.txt") =>
        MergeStrategy.first
      case PathList("META-INF", "services", _*) =>
        MergeStrategy.concat
      case x =>
        val old = (assembly / assemblyMergeStrategy).value
        old(x)
    }
  )

  /** Dependency filter matching Maven shade excludes */
  val assemblyExcludes: Seq[Setting[_]] = Seq(
    assembly / assemblyExcludedJars := {
      val cp = (assembly / fullClasspath).value
      cp.filter { entry =>
        val name = entry.data.getName
        name.startsWith("commons-codec") ||
        name.startsWith("commons-compress") ||
        name.startsWith("commons-io") ||
        name.startsWith("commons-lang3") ||
        name.startsWith("jackson-") ||
        name.startsWith("log4j-") ||
        name.startsWith("slf4j-") ||
        name.startsWith("zstd-jni") ||
        name.startsWith("scala-library") ||
        name.startsWith("scala-reflect")
      }
    }
  )
}
