package com.logingestor.consumer.storage

import com.logingestor.consumer.model.LogEntry
import org.apache.http.HttpHost
import org.elasticsearch.client.{Request, RestClient}
import org.slf4j.{Logger, LoggerFactory}
import play.api.libs.json.{JsString, Json}

import scala.util.Try

class ElasticsearchBatchWriter(host: String, port: Int, scheme: String = "http", indexName: String = "logs") extends LogStorageWriter {

  private val logger: Logger = LoggerFactory.getLogger(getClass)

  private val client: RestClient = RestClient.builder(new HttpHost(host, port, scheme)).build()

  ensureIndex()

  override def write(entries: Seq[LogEntry]): Either[String, Int] = {
    if (entries.isEmpty) return Right(0)

    try {
      val bulkBody = entries.flatMap { e =>
        val action = Json.obj("index" -> Json.obj("_index" -> indexName))
        val source = Json.obj(
          "level" -> e.level,
          "message" -> e.message,
          "resourceId" -> e.resourceId,
          "timestamp" -> e.timestamp,
          "traceId" -> e.traceId,
          "spanId" -> e.spanId,
          "commit" -> e.commit,
          "parentResourceId" -> JsString(e.parentResourceId.getOrElse(""))
        )
        Seq(action.toString() + "\n", source.toString() + "\n")
      }.mkString

      val request = new Request("POST", "/_bulk")
      request.setJsonEntity(bulkBody)

      val response = client.performRequest(request)
      val statusCode = response.getStatusLine.getStatusCode

      if (statusCode >= 200 && statusCode < 300) {
        logger.debug("Indexed {} entries into Elasticsearch index {}", entries.length, indexName)
        Right(entries.length)
      } else {
        val msg = s"ES bulk write failed with status $statusCode: ${response.getStatusLine.getReasonPhrase}"
        logger.error(msg)
        Left(msg)
      }
    } catch {
      case e: Exception =>
        val msg = s"ES bulk write error: ${e.getMessage}"
        logger.error(msg)
        Left(msg)
    }
  }

  override def close(): Unit = {
    try {
      client.close()
      logger.info("Elasticsearch client closed")
    } catch {
      case e: Exception => logger.warn("Error closing ES client: {}", e.getMessage)
    }
  }

  private def ensureIndex(): Unit = {
    try {
      val existsRequest = new Request("HEAD", s"/$indexName")
      val exists = Try(client.performRequest(existsRequest).getStatusLine.getStatusCode == 200).getOrElse(false)

      if (!exists) {
        val mapping = Json.obj(
          "settings" -> Json.obj(
            "number_of_shards" -> 3,
            "number_of_replicas" -> 1
          ),
          "mappings" -> Json.obj(
            "dynamic" -> "strict",
            "properties" -> Json.obj(
              "level" -> Json.obj("type" -> "keyword"),
              "message" -> Json.obj("type" -> "text"),
              "resourceId" -> Json.obj("type" -> "keyword"),
              "timestamp" -> Json.obj("type" -> "date"),
              "traceId" -> Json.obj("type" -> "keyword"),
              "spanId" -> Json.obj("type" -> "keyword"),
              "commit" -> Json.obj("type" -> "keyword"),
              "parentResourceId" -> Json.obj("type" -> "keyword")
            )
          )
        )

        val createRequest = new Request("PUT", s"/$indexName")
        createRequest.setJsonEntity(mapping.toString())
        val response = client.performRequest(createRequest)
        logger.info("Created Elasticsearch index '{}' with mapping — status: {}",
          indexName, response.getStatusLine.getStatusCode)
      } else {
        logger.info("Elasticsearch index '{}' already exists", indexName)
      }
    } catch {
      case e: Exception =>
        logger.warn("Could not ensure ES index '{}': {}. Will retry on first write.", indexName, e.getMessage)
    }
  }
}
