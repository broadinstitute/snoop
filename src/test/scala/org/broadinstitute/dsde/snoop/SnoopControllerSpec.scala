package org.broadinstitute.dsde.snoop

import java.sql.Timestamp

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
          val sInsert = da.insertSubmission(Submission("f00ba4", Option(new Timestamp(System.currentTimeMillis())), null, "gs://test_location", "Submitted"))
          val sSelect = da.getSubmissionBySubmissionId("f00ba4")

          sInsert.id shouldNot be(empty)
          sSelect.submissionId should be(sInsert.submissionId)
        }
      }
    }
  }
}
