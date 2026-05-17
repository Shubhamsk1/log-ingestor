name := "log_ingestor_consumer"
version := "1.0"
scalaVersion := "2.13.10"

enablePlugins(JavaAppPackaging)

libraryDependencies ++= Seq(
  "org.apache.kafka" % "kafka-clients" % "3.5.0",
  "org.scalikejdbc" %% "scalikejdbc" % "3.5.0",
  "org.postgresql" % "postgresql" % "42.3.6",
  "com.typesafe.play" %% "play-json" % "2.10.0-RC7",
  "joda-time" % "joda-time" % "2.11.1",
  "ch.qos.logback" % "logback-classic" % "1.4.14",
  "org.slf4j" % "slf4j-api" % "2.0.9",
  "org.elasticsearch.client" % "elasticsearch-rest-client" % "8.10.2"
)

mainClass := Some("com.logingestor.consumer.LogConsumer")
