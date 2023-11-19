package dao

import models.LogInput
import org.joda.time.DateTime
import scalikejdbc.{DB, scalikejdbcSQLInterpolationImplicitDef}
import scala.util.Try
import scalikejdbc._

import scala.concurrent.{ExecutionContext, Future}

class LogDao {

  private def insertIntoDb(logEntry: LogInput): Int = {

    val timestamp = logEntry.timestamp
    DB.autoCommit { implicit session =>
      sql"""
            insert into logs (level, message, resource_id, timestamp, trace_id, span_id, commit, parent_resource_id)
            values (${logEntry.level}, ${logEntry.message}, ${logEntry.resource_id}, ${timestamp},
              ${logEntry.trace_id}, ${logEntry.span_id}, ${logEntry.commit}, ${logEntry.metadata.map(_.parent_resource_id)})
          """
        .update().apply()
    }
  }

  def insertDao(batch:Seq[LogInput])(implicit ec:ExecutionContext) = Future {
    DB.localTx { implicit session =>

      batch.foreach { log =>
        insertIntoDb(logEntry = log)
      }
    }
  }

  def getLogsByLevel(level: String): Try[List[LogInput]] = Try {
    DB.autoCommit { implicit session =>
      sql"""
            Select * from logs where level like ${level};
          """
        .map(rs => LogInput.fromDb(rs = rs))
        .list()
        .apply()

    }

  }

  def getLogsByMessage(search_string : String):Try[List[LogInput]] = Try {
    DB.autoCommit { implicit session =>
      sql"""
            SELECT * FROM logs WHERE message LIKE ${s"%$search_string%"} ;
         """
        .map(rs => LogInput.fromDb(rs = rs))
        .list()
        .apply()

    }
  }

  def getLogsByResource(resource_id: String): Try[List[LogInput]] = Try {
    DB.autoCommit { implicit session =>
      sql"""
                Select * from logs where resource_id like $resource_id ;
              """
        .map(rs => LogInput.fromDb(rs = rs))
        .list()
        .apply()

    }
  }

  def getLogsByRange(fromTime: DateTime, tillTime:DateTime) = Try {
    DB.autoCommit { implicit session =>
      sql"""
                Select * from logs where timestamp BETWEEN $fromTime and $tillTime ;
              """
        .map(rs => LogInput.fromDb(rs = rs))
        .list()
        .apply()

    }
  }



}
