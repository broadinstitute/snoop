package org.broadinstitute.dsde.snoop.ws

import java.net.URI
import spray.json.DefaultJsonProtocol
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
    requestString: Map[String, String])

object WorkflowExecutionJsonSupport extends DefaultJsonProtocol with SprayJsonSupport {
  implicit val AnalysisFormat = jsonFormat6(WorkflowExecution)
  implicit val ZamboniSubmissionFormat = jsonFormat3(ZamboniSubmission)
}