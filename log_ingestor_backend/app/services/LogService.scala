package services

import dao.LogDao
import models.LogInput
import org.joda.time.DateTime
import scalikejdbc.{DB, scalikejdbcSQLInterpolationImplicitDef}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}


class LogService @Inject()(logDao: LogDao){
  def insertLogs(logEntries: Seq[LogInput])(implicit ec: ExecutionContext): Future[Seq[Unit]] = {
    // Chunk log entries into batches
    val batchSize: Int = 100
    val batches: Seq[Seq[LogInput]] = logEntries.grouped(batchSize).toSeq

    // Use Future.sequence to execute each batch within a local transaction
    Future.sequence(batches.map { batch =>
      logDao.insertDao(batch = batch).map(res =>{
        println("Success full")
      }).recover{case e =>
        println(s"Error Occurred  :: ${e.printStackTrace()}")
        throw e
      }
    })
  }

  def getLogsByLevel(level : Option[String]): Future[Seq[LogInput]] = {
    if(level.isEmpty) {
      println("Level is missing from query")
      Future.failed(new Exception("Missing level"))
    }else{
      Future.fromTry(logDao.getLogsByLevel(level  = level.get))
    }
  }

  def getLogsByMessage(search_str: Option[String]): Future[Seq[LogInput]] = {
    if (search_str.isEmpty) {
      println("search_str is missing from query")
      Future.failed(new Exception("Missing level"))
    } else {
      Future.fromTry(logDao.getLogsByMessage(search_string = search_str.get))
    }
  }

  def getLogsByResource(resource_id: Option[String]): Future[Seq[LogInput]] = {
    if (resource_id.isEmpty) {
      println("resource_id is missing from query")
      Future.failed(new Exception("Missing level"))
    } else {
      Future.fromTry(logDao.getLogsByResource(resource_id = resource_id.get))
    }
  }

  def getLogsByRange(fromTime: Option[DateTime], tillTime:Option[DateTime]): Future[Seq[LogInput]] = {
    if (fromTime.isEmpty ||  tillTime.isEmpty) {
      println("fromTime or tillTime is missing from query")
      Future.failed(new Exception("Missing level"))
    } else {
      Future.fromTry(logDao.getLogsByRange(fromTime = fromTime.get,tillTime = tillTime.get))
    }
  }

}
