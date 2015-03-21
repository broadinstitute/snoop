package org.broadinstitute.dsde.snoop.ws

import org.broadinstitute.dsde.snoop.ws.WorkflowParameter.WorkflowParameter
import spray.json._

object WorkflowParameter {
  type WorkflowParameter = Either[String, Seq[String]]

  def apply(value: String): WorkflowParameter = Left(value)
  def apply(array: Seq[String]): WorkflowParameter = Right(array)
}

case class WorkflowExecution(
    id: Option[String], 
    workflowParameters: Map[String, WorkflowParameter],
    workflowId: String, 
    callbackUri: String,
    status: Option[String]) {
}

object WorkflowExecutionJsonSupport extends DefaultJsonProtocol {
  implicit val AnalysisFormat = jsonFormat5(WorkflowExecution)
}