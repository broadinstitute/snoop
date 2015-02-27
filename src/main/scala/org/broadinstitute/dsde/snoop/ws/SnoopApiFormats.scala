package org.broadinstitute.dsde.snoop.ws

import spray.json.{JsonFormat, DefaultJsonProtocol}
import spray.httpx.SprayJsonSupport

class SnoopApiFormats {
  
}

case class WorkflowExecution(
    id: Option[String], 
    workflowParameters: Map[String, String], 
    runtimeParameters: Option[Map[String, String]], 
    workflowId: String, 
    callbackUri: String,
    status: Option[String])

case class ZamboniSubmission(
    submissionId: String,
    authToken: String,
    requestString: String)

case class ZamboniSubmissionResult(
    submissionId: String,
    status: String)

object WorkflowExecutionJsonSupport extends DefaultJsonProtocol {
  implicit val AnalysisFormat = jsonFormat6(WorkflowExecution)
  implicit val ZamboniSubmissionFormat = jsonFormat3(ZamboniSubmission)
  implicit def ZamboniSubmissionResultFormat = jsonFormat2(ZamboniSubmissionResult)
}