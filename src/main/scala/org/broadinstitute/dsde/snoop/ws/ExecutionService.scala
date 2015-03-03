package org.broadinstitute.dsde.snoop.ws



import akka.actor.{Actor, ActorRef}
import akka.event.Logging

import spray.routing.RequestContext
import spray.httpx.SprayJsonSupport
import spray.client.pipelining._

import scala.util.{ Success, Failure }

/**
 *
 */
object ExecutionService {
  case class Process(submission: ZamboniSubmission)
}
class ExecutionService(requestContext: RequestContext) extends Actor {
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
    log.info("Submitting workflow: id:{}", submissionMessage.submissionId)

    import WorkflowExecutionJsonSupport._
    import SprayJsonSupport._
    val pipeline = sendReceive ~> unmarshal[ZamboniSubmissionResult]

    val responseFuture = pipeline {
      Post(s"http://picard02.openstack.broadinstitute.org:9262/submit", submissionMessage)
    }
    responseFuture onComplete {
      case Success(ZamboniSubmissionResult(submissionId, status)) =>
        log.info("The submissionId is: {} with status {}", submissionId, status)
        requestContext.complete(ZamboniSubmissionResult(submissionId, status))

      case Failure(error) =>
        requestContext.complete(error)
    }
  }


}
