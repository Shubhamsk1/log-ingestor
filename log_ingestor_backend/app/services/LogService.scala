package services

import dao.LogDao
import models.LogEntry
import scalikejdbc.{DB, scalikejdbcSQLInterpolationImplicitDef}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}


class LogService @Inject()(logDao: LogDao){
  def insertLogs(logEntries: Seq[LogEntry])(implicit ec: ExecutionContext): Future[Seq[Unit]] = {
    // Chunk log entries into batches
    val batchSize: Int = 100
    val batches: Seq[Seq[LogEntry]] = logEntries.grouped(batchSize).toSeq

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
}
