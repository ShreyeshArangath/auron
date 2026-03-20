import sbt._

object SparkVersions {

  case class SparkProfile(
      shortVersion: String,
      sparkVersion: String,
      nettyVersion: String,
      scalaTestVersion: String,
      shimName: String
  )

  val profiles: Map[String, SparkProfile] = Map(
    "3.3" -> SparkProfile("3.3", "3.3.4", "4.1.74.Final", "3.2.9", "spark-3.3"),
    "3.4" -> SparkProfile("3.4", "3.4.4", "4.1.87.Final", "3.2.9", "spark-3.4"),
    "3.5" -> SparkProfile("3.5", "3.5.8", "4.1.96.Final", "3.2.9", "spark-3.5"),
    "4.1" -> SparkProfile("4.1", "4.1.1", "4.1.118.Final", "3.2.9", "spark-4.1")
  )

  val activeKey: String = sys.props.getOrElse("spark.version", "3.5")

  lazy val active: SparkProfile = profiles.getOrElse(
    activeKey,
    sys.error(s"Unsupported spark.version=$activeKey. Supported: ${profiles.keys.mkString(", ")}")
  )
}
