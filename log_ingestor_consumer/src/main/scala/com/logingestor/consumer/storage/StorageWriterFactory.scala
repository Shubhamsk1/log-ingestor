package com.logingestor.consumer.storage

import org.slf4j.{Logger, LoggerFactory}

object StorageWriterFactory {

  private val logger: Logger = LoggerFactory.getLogger(getClass)

  sealed trait Mode
  case object PostgresOnly extends Mode
  case object ElasticsearchOnly extends Mode
  case object Both extends Mode

  def createWriters(): Seq[LogStorageWriter] = {
    val modeStr = sys.env.getOrElse("STORAGE_WRITER", "postgres").toLowerCase

    val mode: Mode = modeStr match {
      case "postgres" | "pg" =>
        logger.info("Storage mode: Postgres only")
        PostgresOnly
      case "elasticsearch" | "es" =>
        logger.info("Storage mode: Elasticsearch only")
        ElasticsearchOnly
      case "both" | "all" =>
        logger.info("Storage mode: Postgres + Elasticsearch (dual-write)")
        Both
      case other =>
        logger.warn("Unknown STORAGE_WRITER '{}', defaulting to postgres", other)
        PostgresOnly
    }

    mode match {
      case PostgresOnly =>
        Seq(new PostgresBatchWriter())
      case ElasticsearchOnly =>
        Seq(createEsWriter())
      case Both =>
        Seq(new PostgresBatchWriter(), createEsWriter())
    }
  }

  private def createEsWriter(): LogStorageWriter = {
    val host = sys.env.getOrElse("ES_HOST", "localhost")
    val port = sys.env.get("ES_PORT").flatMap(v => scala.util.Try(v.toInt).toOption).getOrElse(9200)
    val scheme = sys.env.getOrElse("ES_SCHEME", "http")
    val index = sys.env.getOrElse("ES_INDEX", "logs")
    logger.info("Elasticsearch writer configured — {}://{}:{}/{}", scheme, host, port, index)
    new ElasticsearchBatchWriter(host, port, scheme, index)
  }
}
