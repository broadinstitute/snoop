package org.broadinstitute.dsde.snoop.dataaccess

import java.sql.Timestamp

import org.broadinstitute.dsde.snoop.model.Submission


trait SubmissionComponent {
  this: DriverComponent =>
  import driver.simple._

  class Submissions(tag: Tag) extends Table[Submission](tag, "SUBMISSION") {
    def id = column[Int]("ID", O.PrimaryKey, O.AutoInc)
    def submissionId = column[String]("SUBMISSION_ID")
    def submissionDate = column[Timestamp]("SUBMISSION_DATE")
    def modifiedDate = column[Timestamp]("MODIFIED_DATE")
    def callbackUri = column[String]("CALLBACK_URI")
    def status = column[String]("STATUS")

    override def * = (submissionId, submissionDate.?, modifiedDate.?, callbackUri, status, id.?) <> (Submission.tupled, Submission.unapply)
  }

  val submissions = TableQuery[Submissions]
  val submissionsCompiled = Compiled(submissions)

  private val submissionsAutoInc = submissionsCompiled returning submissions.map(_.id) into {
    case (s, id) => s.copy(id = Option(id))
  }
  //private val autoInc = submissions.map(s => (s.submissionId, s.submissionDate, s.modifiedDate, s.callbackUri, s.status)) returning submissions.map(_.id)

  def insertSubmission(submission: Submission)(implicit session: Session): Submission = {
    //submissions.insert(submission)
    //val id = autoInc.insert(submission.submissionId, submission.submissionDate, submission.modifiedDate, submission.callbackUri, submission.status)
    //submission.copy(id = id)
    submissionsAutoInc.insert(submission)
  }

  private val submissionsBySubmissionId = Compiled(
    (submissionId: Column[String]) => for {
      submission <- submissions
      if submission.submissionId === submissionId
    } yield submission)

  private val submissionsBySubmissionIdModified = Compiled(
    (submissionId: Column[String]) => for {
      submission <- submissions
      if submission.submissionId === submissionId
    } yield (submission.status, submission.modifiedDate))

  def updateSubmission(submissionId: String, status: String)(implicit session: Session) {
    submissionsBySubmissionIdModified(submissionId).update(status, new Timestamp(System.currentTimeMillis()))
  }

  def getSubmissionBySubmissionId(submissionId: String)(implicit session: Session): Submission = {
    submissionsBySubmissionId(submissionId).first
  }
}
