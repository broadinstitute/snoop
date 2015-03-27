package org.broadinstitute.dsde.snoop

import java.sql.Timestamp

import org.broadinstitute.dsde.snoop.data.{Submission, SnoopSubmissionController}

/**
 * Created by plin on 3/25/15.
 */
class SnoopControllerSpec extends SnoopDatabaseSpec {
  def actorRefFactory = system

  "SnoopController" - {
    "should insert and retrieve an attribute" in {
      val da = SnoopSubmissionController.dataAccess
      val db = SnoopSubmissionController.database

      db withTransaction {
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
