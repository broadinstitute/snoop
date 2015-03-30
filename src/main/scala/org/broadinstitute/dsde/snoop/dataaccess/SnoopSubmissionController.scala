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

  def createSubmission(workflowId: String, callbackUri: String, status: String) : Submission = {
    database() withTransaction {
      implicit session =>
        val submission = dataAccess.insertSubmission(Submission(workflowId, Option(new Timestamp(System.currentTimeMillis())), null, callbackUri, status))
        submission
    }
  }
}
