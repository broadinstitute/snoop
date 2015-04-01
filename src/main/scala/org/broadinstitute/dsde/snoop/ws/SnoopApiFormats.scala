package org.broadinstitute.dsde.snoop.ws

import org.broadinstitute.dsde.snoop.ws.WorkflowParameter.WorkflowParameter
import spray.json._
import com.wordnik.swagger.annotations.{ApiModel, ApiModelProperty}
import scala.annotation.meta.field

object WorkflowParameter {
  type WorkflowParameter = Either[String, Seq[String]]

  def apply(value: String): WorkflowParameter = Left(value)
  def apply(array: Seq[String]): WorkflowParameter = Right(array)
}

@ApiModel(value = "Start Workflow / Workflow Status Response")
case class WorkflowExecution(
    @(ApiModelProperty@field)(required = false, value = "The uuid of the workflow execution. Required for all but POST requests.")
    id: Option[String], 
    @(ApiModelProperty@field)(required = true, value = "The workflow parameters. Values may be Strings or Lists of Strings.")
    workflowParameters: Map[String, WorkflowParameter] = Map.empty,
    @(ApiModelProperty@field)(required = true, value = "The workflow id to run.")
    workflowId: String = "",
    @(ApiModelProperty@field)(required = true, value = "The callback uri. This URI will be POSTed to when the workflow execution completes.")
    callbackUri: String = "",
    @(ApiModelProperty@field)(required = false, value = "The workflow status.")
    status: Option[String])

object WorkflowExecutionJsonSupport extends DefaultJsonProtocol {
  implicit val AnalysisFormat = jsonFormat5(WorkflowExecution)
}
