package org.broadinstitute.dsde.snoop.ws

import spray.json.DefaultJsonProtocol

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
    authToken: String,
    requestString: String)

case class ZamboniSubmissionResult(
    workflowId: String,
    status: String)


object WorkflowExecutionJsonSupport extends DefaultJsonProtocol {
  implicit val AnalysisFormat = jsonFormat5(WorkflowExecution)
  implicit val ZamboniSubmissionFormat = jsonFormat2(ZamboniSubmission)
  implicit val ZamboniSubmissionResultFormat = jsonFormat2(ZamboniSubmissionResult)
  implicit val ZamboniWorkflowFormat = jsonFormat2(ZamboniWorkflow)
}