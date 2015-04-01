package org.broadinstitute.dsde.snoop.ws


import akka.actor.ActorSystem
import org.broadinstitute.dsde.snoop.dataaccess.SnoopSubmissionController
import org.broadinstitute.dsde.snoop.model.Submission
import org.broadinstitute.dsde.snoop.ws.WorkflowParameter.WorkflowParameter
import spray.httpx.SprayJsonSupport
import spray.client.pipelining._
import org.broadinstitute.dsde.snoop.{OutputRepository, SnoopException, WorkflowExecutionService}
import scala.concurrent.Await
import scala.concurrent.duration._

import SprayJsonSupport._
import spray.json._

case class ZamboniSubmission(requestString: String)
case class ZamboniSubmissionResult(workflowId: String, status: String)

object ZamboniJsonSupport extends DefaultJsonProtocol {
  implicit val ZamboniSubmissionFormat = jsonFormat1(ZamboniSubmission)
  implicit val ZamboniSubmissionResultFormat = jsonFormat2(ZamboniSubmissionResult)
}

import ZamboniJsonSupport._

trait ZamboniApi {
  def start(zamboniSubmission: ZamboniSubmission): ZamboniSubmissionResult
  def status(zamboniId: String): ZamboniSubmissionResult
}

/**
 * Zamboni API representation for standard (i.e. non-stubbed) purposes
 */
case class StandardZamboniApi(zamboniServer: String)(implicit val system: ActorSystem) extends ZamboniApi {
  import system.dispatcher
  
  def start(zamboniSubmission: ZamboniSubmission): ZamboniSubmissionResult = {
    val pipeline = sendReceive ~> unmarshal[ZamboniSubmissionResult]

    val future = pipeline {
      Post(s"$zamboniServer/submit", zamboniSubmission)
    }

    Await.result(future, 1 minutes)
  }

  def status(zamboniId: String): ZamboniSubmissionResult = {
    val pipeline = sendReceive ~> unmarshal[ZamboniSubmissionResult]

    val future = pipeline {
      Get(s"$zamboniServer/status/$zamboniId")
    }

    Await.result(future, 1 minutes)
  }
}

case class ZamboniWorkflowExecutionService(zamboniApi: ZamboniApi,
                                           gcsSandboxBucket: String,
                                           gcsSandboxKeyPrefix: String,
                                           snoopSubmissionController: SnoopSubmissionController,
                                           callbackHandler: AnalysisCallbackHandler,
                                           outputRepository: OutputRepository) extends WorkflowExecutionService {
  import WorkflowExecutionJsonSupport._

  def start(workflowExecution: WorkflowExecution, securityToken: String, submissionSandbox: String) : String = {
    log.info("Submitting workflow: " + workflowExecution.toJson)

    val response = zamboniApi.start(snoop2ZamboniWorkflow(workflowExecution, securityToken, submissionSandbox))
    response.status
  }
  
  def status(id: String, securityToken: String): String = {
    log.info("Getting status for workflow: " + id)
    val response = zamboniApi.status(id)
    log.info("The workflowId is: {} with status {}", response.workflowId, response.status)
    response.status
  }

  def snoop2ZamboniWorkflow(exeMessage: WorkflowExecution, securityToken: String, submissionSandbox: String) : ZamboniSubmission = {
    val workflowParameters: Map[String, WorkflowParameter] =
      exeMessage.workflowParameters ++
        Map(("gcsSandboxBucket" -> WorkflowParameter(submissionSandbox)),
            ("authToken" -> WorkflowParameter(securityToken)))
    import WorkflowExecutionJsonSupport._
    val workflowJson = workflowParameters.toJson

    val zamboniRequest = JsObject(Map(
      ("Zamboni" -> JsObject(Map("workflow"-> JsString(exeMessage.workflowId)))),
      ("workflow" -> workflowJson)
    ))

    val zamboniRequestString = zamboniRequest.toJson.toString
    ZamboniSubmission(zamboniRequestString)
  }

}
