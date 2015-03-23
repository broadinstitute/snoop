package org.broadinstitute.dsde.snoop.ws

import org.broadinstitute.dsde.snoop.ws.WorkflowParameter.WorkflowParameter
import spray.json._

object WorkflowParameter {
  type WorkflowParameter = Either[String, Seq[String]]

  def apply(value: String): WorkflowParameter = Left(value)
  def apply(array: Seq[String]): WorkflowParameter = Right(array)
}

import com.wordnik.swagger.annotations.{ApiModel, ApiModelProperty}
import scala.annotation.meta.field

@ApiModel(value = "Start Workflow / Workflow Status Response")
case class WorkflowExecution(
    @(ApiModelProperty@field)(required = false, value = "The id")
    id: Option[String], 
    @(ApiModelProperty@field)(required = true, value = "The workflow parameters")
    workflowParameters: Map[String, WorkflowParameter],
    @(ApiModelProperty@field)(required = true, value = "The workflow id")
    workflowId: String, 
    @(ApiModelProperty@field)(required = true, value = "The callback uri")
    callbackUri: String,
    @(ApiModelProperty@field)(required = false, value = "The workflow status")
    status: Option[String])

object WorkflowExecutionJsonSupport extends DefaultJsonProtocol {
  implicit val AnalysisFormat = jsonFormat5(WorkflowExecution)
}
