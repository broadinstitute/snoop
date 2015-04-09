package org.broadinstitute.dsde.snoop.dataaccess

import java.sql.Timestamp

import org.broadinstitute.dsde.snoop.model.Submission

trait SubmissionComponent {
  this: DriverComponent =>
  import driver.simple._

  class Submissions(tag: Tag) extends Table[Submission](tag, "SUBMISSION") {
    def submissionId = column[String]("SUBMISSION_ID")
    def submissionDate = column[Timestamp]("SUBMISSION_DATE")
    def modifiedDate = column[Timestamp]("MODIFIED_DATE")
    def callbackUri = column[String]("CALLBACK_URI")
    def status = column[String]("STATUS")
    def id = column[String]("ID", O.PrimaryKey)

    override def * = (id, submissionId, submissionDate.?, modifiedDate.?, callbackUri.?, status) <> (Submission.tupled, Submission.unapply)
  }

  val submissions = TableQuery[Submissions]
  val submissionsCompiled = Compiled(submissions)

  def insertSubmission(id: String, submissionId: String, callbackUri: Option[String], status: String)(implicit session: Session): Submission = {
    val submission = Submission(id, submissionId, Option(new Timestamp(System.currentTimeMillis())),
                                null, callbackUri, status)
    submissionsCompiled += submission
    submission
  }

  private val submissionsById = Compiled(
    (id: Column[String]) => for {
      submission <- submissions
      if submission.id === id
    } yield submission)

  private val submissionsByIdModified = Compiled(
    (id: Column[String]) => for {
      submission <- submissions
      if submission.id === id
    } yield (submission.status, submission.modifiedDate))

  def updateSubmission(id: String, status: String)(implicit session: Session) {
    submissionsByIdModified(id).update(status, new Timestamp(System.currentTimeMillis()))
  }

  def getSubmissionById(id: String)(implicit session: Session): Submission = {
    submissionsById(id).first
  }
}
