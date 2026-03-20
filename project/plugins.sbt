addSbtPlugin("com.thesamet" % "sbt-protoc" % "1.0.7")
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.5.2")
addSbtPlugin("com.beautiful-scala" % "sbt-scalastyle" % "1.5.1")
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "2.3.0")

libraryDependencies += "com.thesamet.scalapb" %% "compilerplugin" % "0.11.17"
