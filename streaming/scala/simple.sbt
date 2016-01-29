name := "Simple Project"

version := "1.0"

scalaVersion := "2.10.5"

libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % "1.6.0",
  "com.google.code.gson" % "gson" % "2.2",
  "com.twitter"%"algebird-core_2.10"% "0.11.0"
)
