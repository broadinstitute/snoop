package org.broadinstitute.dsde.snoop.ws



import akka.actor.{Actor, ActorRef}
import akka.event.Logging
import spray.json._
import spray.routing.RequestContext
import spray.httpx.SprayJsonSupport
import spray.client.pipelining._
import WorkflowExecutionJsonSupport._

import scala.util.{ Success, Failure }

/**
 *
 */
object ExecutionService {
  case class Process(submission: ZamboniSubmission)
}
case class ExecutionService(requestContext: RequestContext) extends Actor {
  import ExecutionService._

  implicit val system = context.system
  import system.dispatcher
  val log = Logging(system, getClass)

  override def receive = {
    case Process(submission) =>
      process(submission)
      context.stop(self)
  }

  def process(submissionMessage: ZamboniSubmission) : Unit = {
    log.info("Submitting workflow: ", submissionMessage.requestString)

    import SprayJsonSupport._

    val pipeline = sendReceive ~> unmarshal[ZamboniSubmissionResult]

    val responseFuture = pipeline {
      Post(s"http://vzamboni-ces.broadinstitute.org:9262/submit", submissionMessage)
    }
    responseFuture onComplete {
      case Success(ZamboniSubmissionResult(workflowId, status)) =>
        log.info("The workflowId is: {} with status {}", workflowId, status)
        requestContext.complete(zamMessages2Snoop(submissionMessage, ZamboniSubmissionResult(workflowId, status)))

      case Failure(error) =>
        requestContext.complete(error)
    }
  }

  def zamMessages2Snoop(submissionMessage: ZamboniSubmission, zamResponse: ZamboniSubmissionResult): WorkflowExecution = {
    val requestString: ZamboniWorkflow = submissionMessage.requestString.parseJson.convertTo[ZamboniWorkflow]

    WorkflowExecution(Option(zamResponse.workflowId), Map.empty, requestString.Zamboni("workflow"), "callback", Option(zamResponse.status))
  }

}