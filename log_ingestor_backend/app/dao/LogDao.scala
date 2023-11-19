package dao

import models.LogEntry
import org.joda.time.DateTime
import scalikejdbc.{DB, scalikejdbcSQLInterpolationImplicitDef}
import scalikejdbc._

import scala.concurrent.{ExecutionContext, Future}

class LogDao {

  private def insertIntoDb(logEntry: LogEntry): Int = {

    val timestamp = DateTime.parse(logEntry.timestamp)
    DB.autoCommit { implicit session =>
      sql"""
            insert into logs (level, message, resource_id, timestamp, trace_id, span_id, commit, parent_resource_id)
            values (${logEntry.level}, ${logEntry.message}, ${logEntry.resourceId}, ${timestamp},
              ${logEntry.traceId}, ${logEntry.spanId}, ${logEntry.commit}, ${logEntry.metadata.map(_.parentResourceId)})
          """
        .update().apply()
    }
  }

  def insertDao(batch:Seq[LogEntry])(implicit ec:ExecutionContext) = Future {
    DB.localTx { implicit session =>

      batch.foreach { log =>
        insertIntoDb(logEntry = log)
      }
    }
  }


//  def insertSQL(logEntry: LogEntry) = {
//      sql"""
//         insert into logs (level, message, resource_id, timestamp, trace_id, span_id, commit, parent_resource_id)
//         values (${logEntry.level}, ${logEntry.message}, ${logEntry.resourceId}, ${logEntry.timestamp},
//        ${logEntry.traceId}, ${logEntry.spanId}, ${logEntry.commit}, ${logEntry.metadata.map(_.parentResourceId)})
//       """.stripMargin
//  }
//  def insertLogsAsync(logEntries: Seq[LogEntry]): Future[Int] = {
//
//    val batchSize = 100
//    val batches: Seq[Seq[LogEntry]] = logEntries.grouped(batchSize).toSeq
//
//    val insertSQL =
//      sql"""
//                INSERT INTO logs (level, message, resource_id,timestamp,trace_id,span_id,commit,parent_resource_id)
//                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
//              """.stripMargin
//
//    // Use Future.sequence to execute the entire batch within a local transaction
//
//      Future.sequence(batches.map{ batch =>
//        Async.localTx { implicit
//        val insertSQL =
//          sql"""
//            INSERT INTO logs (level, message, timestamp, resourceId)
//            VALUES (?, ?, ?, ?)
//          """.stripMargin
//
//          batch.foreach { log =>
//            insertSQL.bind(log.level, log.message, log.timestamp, log.resourceId).update.apply()
//          }
//        }
//
//      })
//
//  }

}
