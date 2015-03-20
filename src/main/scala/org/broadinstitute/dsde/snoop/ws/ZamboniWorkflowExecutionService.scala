package org.broadinstitute.dsde.snoop.ws


import java.util.UUID

import akka.actor.{Props, Actor, ActorRef, ActorSystem}
import akka.event.Logging
import com.typesafe.config.Config
import spray.json._
import spray.routing.RequestContext
import spray.httpx.SprayJsonSupport
import spray.client.pipelining._
import WorkflowExecutionJsonSupport._
import org.broadinstitute.dsde.snoop.WorkflowExecutionService
import scala.concurrent.Future

import scala.util.{ Success, Failure }
import SprayJsonSupport._

trait ZamboniApi {
  def start(zamboniSubmission: ZamboniSubmission): Future[ZamboniSubmissionResult]
  def status(zamboniId: String): Future[ZamboniSubmissionResult]
}

/**
 * Zamboni API representation for standard (i.e. non-stubbed) purposes
 */
case class StandardZamboniApi(zamboniServer: String)(implicit val system: ActorSystem) extends ZamboniApi {
  import system.dispatcher
  
  def start(zamboniSubmission: ZamboniSubmission): Future[ZamboniSubmissionResult] = {
    val pipeline = sendReceive ~> unmarshal[ZamboniSubmissionResult]

    pipeline {
      Post(s"$zamboniServer/submit", zamboniSubmission)
    }
  }

  def status(zamboniId: String): Future[ZamboniSubmissionResult] = {
    val pipeline = sendReceive ~> unmarshal[ZamboniSubmissionResult]

    pipeline {
      Get(s"$zamboniServer/status/$zamboniId")
    }
  }
}

object ZamboniWorkflowExecutionService {
  def apply(zamboniApi: ZamboniApi, gcsSandboxBucket: String)(requestContext: RequestContext): ZamboniWorkflowExecutionService = new ZamboniWorkflowExecutionService(requestContext, zamboniApi, gcsSandboxBucket)
}

case class ZamboniWorkflowExecutionService(requestContext: RequestContext, zamboniApi: ZamboniApi, gcsSandboxBucket: String) extends WorkflowExecutionService {
  import system.dispatcher
  
  def start(workflowExecution: WorkflowExecution) : Unit = {
    log.info("Submitting workflow: ", workflowExecution)

    zamboniApi.start(snoop2ZamboniWorkflow(workflowExecution)) onComplete {
      case Success(response: ZamboniSubmissionResult) =>
        log.info("The workflowId is: {} with status {}", response.workflowId, response.status)
        requestContext.complete(zamMessages2Snoop(workflowExecution, response))

      case Failure(error) =>
        requestContext.complete(error)
    }
  }
  
  def status(id: String) {
    log.info("Getting status for workflow: ", id)
    zamboniApi.status(id) onComplete {
      case Success(response: ZamboniSubmissionResult) =>
        log.info("The workflowId is: {} with status {}", response.workflowId, response.status)
        requestContext.complete(zamMessages2Snoop(WorkflowExecution(None, Map.empty, "workflow_id", "callback", None), response))

      case Failure(error) =>
        requestContext.complete(error)
    }
  }

  def zamMessages2Snoop(workflowExecution: WorkflowExecution, zamResponse: ZamboniSubmissionResult): WorkflowExecution = {
    workflowExecution.copy(id = Option(zamResponse.workflowId), status = Option(zamResponse.status))
  }

  def snoop2ZamboniWorkflow(exeMessage: WorkflowExecution) : ZamboniSubmission = {
    val zamboniRequest = ZamboniWorkflow(Map("workflow"-> exeMessage.workflowId), exeMessage.workflowParameters + ("gcsSandboxBucket" -> (gcsSandboxBucket + UUID.randomUUID().toString)))
    val zamboniRequestString = zamboniRequest.toJson.toString
    val zamboniMessage = ZamboniSubmission("some-token", zamboniRequestString)
    zamboniMessage
  }

}