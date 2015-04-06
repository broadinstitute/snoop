package org.broadinstitute.dsde.snoop.dataaccess

import java.sql.Timestamp
import javax.sql.DataSource

import org.broadinstitute.dsde.snoop.model.Submission
import scala.slick.jdbc.JdbcBackend._

/**
 * Created by plin on 3/25/15.
 */
object SnoopSubmissionController {
  def apply(dataSource: DataSource, slickDriver: String): SnoopSubmissionController = {
    new SnoopSubmissionController(() => Database.forDataSource(dataSource), slickDriver)
  }
}

class SnoopSubmissionController(database: () => Database, slickDriver: String) {
  val dataAccess = new DataAccess(slickDriver)

  def createSubmission(id: String, submissionId: String, callbackUri: String, status: String) : Submission = {
    database() withTransaction {
      implicit session =>
        val submission = dataAccess.insertSubmission(id, submissionId, callbackUri, status)
        submission
    }
  }

  def getSubmission(id: String) : Submission = {
    database() withTransaction {
      implicit session =>
        val submission = dataAccess.getSubmissionById(id)
        submission
    }
  }

  def updateSubmissionStatus(id: String, status: String) : Unit = {
    database() withTransaction {
      implicit session =>
        dataAccess.updateSubmission(id, status)
    }
  }
}
