package org.broadinstitute.dsde.snoop.ws

import spray.json.{JsonFormat, DefaultJsonProtocol}

class SnoopApiFormats {

}

case class WorkflowExecution(
    id: Option[String], 
    workflowParameters: Map[String, String],
    workflowId: String, 
    callbackUri: String,
    status: Option[String])

case class ZamboniWorkflow(
     Zamboni: Map[String, String],
     workflow: Map[String, String])

case class ZamboniSubmission(
    submissionId: String,
    authToken: String,
    requestString: String)

case class ZamboniSubmissionResult(
    submissionId: String,
    status: String)

object WorkflowExecutionJsonSupport extends DefaultJsonProtocol {
  implicit val AnalysisFormat = jsonFormat5(WorkflowExecution)
  implicit val ZamboniSubmissionFormat = jsonFormat3(ZamboniSubmission)
  implicit val ZamboniSubmissionResultFormat = jsonFormat2(ZamboniSubmissionResult)
  implicit val ZamboniWorkflowFormat = jsonFormat2(ZamboniWorkflow)
}