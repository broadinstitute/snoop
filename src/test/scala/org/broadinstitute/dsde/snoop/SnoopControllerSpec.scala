package org.broadinstitute.dsde.snoop

import java.sql.Timestamp
import java.util.UUID

import org.broadinstitute.dsde.snoop.dataaccess.SnoopSubmissionController
import org.broadinstitute.dsde.snoop.model.Submission

import scala.slick.jdbc.JdbcBackend._

/**
 * Created by plin on 3/25/15.
 */
class SnoopControllerSpec extends SnoopDatabaseSpec {
  def actorRefFactory = system

  "SnoopController" - {
    "should insert and retrieve an attribute" in {
      val snoopSubmissionController = new SnoopSubmissionController(() => TestDatabase.db, DatabaseConfig.slickDriver)
      val da = snoopSubmissionController.dataAccess

      TestDatabase.db withTransaction {
        implicit session => {
          val id = UUID.randomUUID.toString
          val sInsert = snoopSubmissionController.createSubmission(id, "f00ba4", Option("foo"), "Submitted")
          val sSelect = da.getSubmissionById(id)

          sSelect.submissionId should be(sInsert.submissionId)
          sSelect.id should be(sInsert.id)
          sSelect.callbackUri should be(sInsert.callbackUri)
          sSelect.status should be(sInsert.status)
        }
      }
    }
  }
}
