/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one or more
 *  * contributor license agreements.  See the NOTICE file distributed with
 *  * this work for additional information regarding copyright ownership.
 *  * The ASF licenses this file to You under the Apache License, Version 2.0
 *  * (the "License"); you may not use this file except in compliance with
 *  * the License.  You may obtain a copy of the License at
 *  *
 *  *    http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 *
 */

import sbt.{ExclusionRule, _}

object Dependencies {

  def scalaReflection(scalaVersion: String): Seq[ModuleID] =
    Seq(
      "org.scala-lang" % "scala-reflect" % scalaVersion
    )

  // Exclusions

  val jacksonExclusions = Seq(
    ExclusionRule(organization = "com.fasterxml.jackson.core"),
    ExclusionRule(organization = "com.fasterxml.jackson.databind"),
    ExclusionRule(organization = "com.fasterxml.jackson.jaxrs"),
    ExclusionRule(organization = "com.fasterxml.jackson.module"),
    ExclusionRule(organization = "com.fasterxml.jackson.dataformat", "jackson-dataformat-yaml")
  )

  val jnaExclusions = Seq(ExclusionRule(organization = "net.java.dev.jna"))

  val sparkExclusions = Seq(
    ExclusionRule(organization = "org.apache.spark")
  )

  // Provided

  val jackson212ForSpark3 = Seq(
    "com.fasterxml.jackson.core" % "jackson-core" % Versions.jackson212ForSpark3 % "provided",
    "com.fasterxml.jackson.core" % "jackson-annotations" % Versions.jackson212ForSpark3 % "provided",
    "com.fasterxml.jackson.core" % "jackson-databind" % Versions.jackson212ForSpark3 % "provided",
    "com.fasterxml.jackson.module" %% "jackson-module-scala" % Versions.jackson212ForSpark3 % "provided",
    "com.fasterxml.jackson.dataformat" % "jackson-dataformat-yaml" % Versions.jackson212ForSpark3 % "provided"
  )

  val spark_3d0_forScala_2d12 = Seq(
    "org.apache.spark" %% "spark-core" % Versions.spark3d0 % "provided" exclude ("com.google.guava", "guava") excludeAll (jacksonExclusions: _*),
    "org.apache.spark" %% "spark-sql" % Versions.spark3d0 % "provided" exclude ("com.google.guava", "guava") excludeAll (jacksonExclusions: _*),
    "org.apache.spark" %% "spark-hive" % Versions.spark3d0 % "provided" exclude ("com.google.guava", "guava") excludeAll (jacksonExclusions: _*),
    "org.apache.spark" %% "spark-mllib" % Versions.spark3d0 % "provided" exclude ("com.google.guava", "guava") excludeAll (jacksonExclusions: _*),
    "com.databricks" %% "spark-xml" % Versions.sparkXML,
    "org.apache.spark" %% "spark-sql-kafka-0-10" % Versions.spark3d0,
    "org.apache.spark" %% "spark-avro" % Versions.spark3d0,
    "io.delta" %% "delta-spark" % Versions.deltaSpark3d0 % "provided" exclude ("com.google.guava", "guava") excludeAll (jacksonExclusions: _*)
  )

  val azure = Seq(
    "org.apache.hadoop" % "hadoop-azure" % "3.3.6" % "provided" excludeAll (jacksonExclusions: _*) exclude ("com.google.guava", "guava"),
    "com.microsoft.azure" % "azure-storage" % "8.6.6" % "provided" excludeAll (jacksonExclusions: _*) exclude ("com.google.guava", "guava")
  )

  val hadoop = Seq(
    "org.apache.hadoop" % "hadoop-common" % Versions.hadoop % "provided" excludeAll (jacksonExclusions: _*) exclude ("com.google.guava", "guava"),
    "org.apache.hadoop" % "hadoop-hdfs" % Versions.hadoop % "provided" excludeAll (jacksonExclusions: _*) exclude ("com.google.guava", "guava"),
    "org.apache.hadoop" % "hadoop-yarn-client" % Versions.hadoop % "provided" excludeAll (jacksonExclusions: _*) exclude ("com.google.guava", "guava"),
    "org.apache.hadoop" % "hadoop-mapreduce-client-app" % Versions.hadoop % "provided" excludeAll (jacksonExclusions: _*) exclude ("com.google.guava", "guava"),
    "org.apache.hadoop" % "hadoop-client" % Versions.hadoop % "provided" excludeAll (jacksonExclusions: _*) exclude ("com.google.guava", "guava")
  )

  val snowflake = Seq(
    "net.snowflake" % "snowflake-jdbc" % Versions.snowflakeJDBC % Test,
    "net.snowflake" %% "spark-snowflake" % Versions.snowflakeSpark % Test
  )

  val redshift = Seq(
    "com.amazon.redshift" % "redshift-jdbc42" % Versions.redshiftJDBC % Test
  )

  val scalaTest = Seq(
    "org.scalatest" %% "scalatest" % Versions.scalatest % Test
  )

  val h2 = Seq(
    "com.h2database" % "h2" % Versions.h2 % Test
  )

  // Included

  val betterfiles = Seq("com.github.pathikrit" %% "better-files" % Versions.betterFiles)

  val logging = Seq(
    "com.typesafe" % "config" % Versions.typesafeConfig,
    "com.typesafe.scala-logging" %% "scala-logging" % Versions.scalaLogging
  )

  val pureConfig212 =
    Seq(
      "com.github.pureconfig" %% "pureconfig" % Versions.pureConfig212ForSpark3
    )

  val gcsConnectorShadedJar =
    s"${Resolvers.googleCloudBigDataMavenRepo}/gcs-connector/${Versions.gcsConnector}/gcs-connector-${Versions.gcsConnector}-shaded.jar"

  val gcpBigQueryConnectorShadedJar =
    s"${Resolvers.googleCloudBigDataMavenRepo}/bigquery-connector/${Versions.bigqueryConnector}/bigquery-connector-${Versions.bigqueryConnector}-shaded.jar"

  val gcp = Seq(
    "com.google.cloud.bigdataoss" % "gcs-connector" % Versions.gcsConnector from gcsConnectorShadedJar exclude ("javax.jms", "jms") exclude ("com.sun.jdmk", "jmxtools") exclude ("com.sun.jmx", "jmxri") excludeAll (jacksonExclusions: _*) classifier "shaded",
    "com.google.cloud.bigdataoss" % "bigquery-connector" % Versions.bigqueryConnector from gcpBigQueryConnectorShadedJar exclude ("javax.jms", "jms") exclude ("com.sun.jdmk", "jmxtools") exclude ("com.sun.jmx", "jmxri") excludeAll (jacksonExclusions: _*) classifier "shaded",
    "com.google.cloud" % "google-cloud-bigquery" % Versions.bigquery exclude ("javax.jms", "jms") exclude ("com.sun.jdmk", "jmxtools") exclude ("com.sun.jmx", "jmxri") excludeAll (jacksonExclusions: _*),
    // see https://github.com/GoogleCloudDataproc/spark-bigquery-connector/issues/36
    // Add the jar file to spark dependencies
    "com.google.cloud.spark" %% "spark-bigquery-with-dependencies" % Versions.sparkBigqueryWithDependencies % "provided" excludeAll (jacksonExclusions: _*),
    "com.google.cloud" % "google-cloud-datacatalog" % Versions.gcpDataCatalog excludeAll (jacksonExclusions: _*),
    "com.google.cloud" % "google-cloud-logging" % Versions.gcpCloudLogging
  )

  val esSpark212 = Seq(
    "org.elasticsearch" %% "elasticsearch-spark-30" % Versions.esSpark212 % "provided" exclude ("com.google.guava", "guava") excludeAll ((sparkExclusions ++ jacksonExclusions): _*),
    "com.dimafeng" %% "testcontainers-scala-elasticsearch" % Versions.testContainers % Test excludeAll (jnaExclusions: _*)
  )

  val scopt = Seq(
    "com.github.scopt" %% "scopt" % Versions.scopt
  )

  val excelClientApi = Seq(
    "org.apache.poi" % "poi-ooxml" % Versions.poi
  )

  val scalate = Seq(
    "org.scalatra.scalate" %% "scalate-core" % Versions.scalate exclude ("org.scala-lang.modules", "scala-xml_2.12")
  )

  val kafkaClients = Seq(
    "org.apache.kafka" % "kafka-clients" % Versions.kafkaClients,
    "io.confluent" % "kafka-schema-registry-client" % Versions.confluentVersion % "provided",
    "io.confluent" % "kafka-avro-serializer" % Versions.confluentVersion % "provided",
    "com.dimafeng" %% "testcontainers-scala-scalatest" % Versions.testContainers % Test excludeAll (jnaExclusions: _*),
    "com.dimafeng" %% "testcontainers-scala-kafka" % Versions.testContainers % Test excludeAll (jnaExclusions: _*)
  )

  val bigQueue = Seq("com.leansoft" % "bigqueue" % Versions.bigQueue)

  val jna_apple_arm_testcontainers = Seq(
    "net.java.dev.jna" % "jna" % "5.12.1"
  )

  val pgGcp = Seq(
    "com.google.cloud.sql" % "postgres-socket-factory" % "1.17.1" % Test,
    "com.dimafeng" %% "testcontainers-scala-postgresql" % Versions.testContainers % Test excludeAll (jnaExclusions: _*),
    "org.postgresql" % "postgresql" % "42.7.2" % Test
  )

  val jinja = Seq(
    "com.hubspot.jinjava" % "jinjava" % Versions.jinja excludeAll (jacksonExclusions: _*) exclude ("com.google.guava", "guava") exclude ("org.apache.commons", "commons-lang3")
  )

  val jSqlParser = Seq("com.github.jsqlparser" % "jsqlparser" % Versions.jSqlParser)

  val dependencies =
    jna_apple_arm_testcontainers ++ scalate ++ logging ++ betterfiles ++ snowflake ++ redshift ++ scalaTest ++ scopt ++ hadoop ++
    gcp ++ azure ++ h2 ++ excelClientApi ++ kafkaClients ++ jinja ++ jSqlParser ++ pgGcp // ++ bigQueue
}
