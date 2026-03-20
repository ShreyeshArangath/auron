import sbt._

object AuronDeps {

  val arrowVersion = "16.0.0"
  val bytebuddyVersion = "1.14.11"
  val hadoopVersion = "3.4.2"
  val protobufVersion = "3.25.5"
  val scalaXmlVersion = "2.1.0"
  val scalaJava8CompatVersion = "1.0.2"
  val junitJupiterVersion = "5.13.4"
  val paradiseVersion = "2.1.1"
  val semanticdbVersion = "4.14.5"

  lazy val spark = SparkVersions.active

  // Arrow
  val arrowCData = "org.apache.arrow" % "arrow-c-data" % arrowVersion
  val arrowCompression = "org.apache.arrow" % "arrow-compression" % arrowVersion
  val arrowMemoryUnsafe = "org.apache.arrow" % "arrow-memory-unsafe" % arrowVersion
  val arrowVector = "org.apache.arrow" % "arrow-vector" % arrowVersion

  val arrowAll = Seq(arrowCData, arrowCompression, arrowMemoryUnsafe, arrowVector)

  // Protobuf
  val protobufJava = "com.google.protobuf" % "protobuf-java" % protobufVersion

  // Hadoop
  val hadoopClientApi = "org.apache.hadoop" % "hadoop-client-api" % hadoopVersion

  // ByteBuddy
  val byteBuddy = "net.bytebuddy" % "byte-buddy" % bytebuddyVersion
  val byteBuddyAgent = "net.bytebuddy" % "byte-buddy-agent" % bytebuddyVersion

  // Netty (version from active Spark profile)
  def nettyBuffer = "io.netty" % "netty-buffer" % spark.nettyVersion
  def nettyCommon = "io.netty" % "netty-common" % spark.nettyVersion

  // Spark (Provided scope)
  def sparkCore(scalaV: String) =
    ("org.apache.spark" %% "spark-core" % spark.sparkVersion)
      .exclude("io.netty", "*")

  def sparkSql(scalaV: String) =
    ("org.apache.spark" %% "spark-sql" % spark.sparkVersion)
      .exclude("io.netty", "*")
      .exclude("org.apache.arrow", "*")

  def sparkHive(scalaV: String) =
    ("org.apache.spark" %% "spark-hive" % spark.sparkVersion)
      .exclude("org.apache.arrow", "*")

  def sparkCatalyst(scalaV: String) =
    "org.apache.spark" %% "spark-catalyst" % spark.sparkVersion

  // Scala compat
  val scalaJava8Compat = ("org.scala-lang.modules" %% "scala-java8-compat" % scalaJava8CompatVersion)
    .exclude("org.scala-lang", "scala-library")

  // Scala XML
  val scalaXml = "org.scala-lang.modules" %% "scala-xml" % scalaXmlVersion

  // Test
  def scalaTest = "org.scalatest" %% "scalatest" % spark.scalaTestVersion
  val junitJupiter = "org.junit.jupiter" % "junit-jupiter-api" % junitJupiterVersion

  // Misc
  val jsr305 = "com.google.code.findbugs" % "jsr305" % "1.3.9"
}
