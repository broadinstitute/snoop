package org.broadinstitute.dsde.snoop

import akka.actor.{Props, Actor}
import com.typesafe.config.Config
import spray.httpx.SprayJsonSupport
import spray.routing._
import spray.json._
import spray.http.MediaTypes._
import spray.httpx.marshalling.ToResponseMarshallable.isMarshallable
import spray.routing.Directive.pimpApply
import org.broadinstitute.dsde.snoop.ws._
import akka.event.Logging

object SnoopApiServiceActor {
  def props(executionServiceHandler: RequestContext => WorkflowExecutionService): Props = {
    Props(new SnoopApiServiceActor(executionServiceHandler))
  }
}

class SnoopApiServiceActor(override val executionServiceHandler: RequestContext => WorkflowExecutionService) extends Actor with SnoopApiService {
  def actorRefFactory = context
  def receive = runRoute(snoopRoute)
}


// this trait defines our service behavior independently from the service actor
trait SnoopApiService extends HttpService {
  implicit def executionContext = actorRefFactory.dispatcher
  import WorkflowExecutionJsonSupport._
  import SprayJsonSupport._

  def executionServiceHandler: RequestContext => WorkflowExecutionService

  val snoopRoute =
    path("") {
      get {
        respondWithMediaType(`text/html`) {
          complete {
            <html>
              <body>
                <h1>Snoop web service is operational</h1>
              </body>
            </html>
          }
        }
      }
    } ~
    path("workflowExecutions") {
      post {
        entity(as[WorkflowExecution]) { workflowExecution =>
          requestContext =>
            val executionService = actorRefFactory.actorOf(WorkflowExecutionService.props(executionServiceHandler, requestContext))
            executionService ! WorkflowExecutionService.WorkflowStart(workflowExecution)
        }
      }
    } ~
    path("workflowExecutions" / Segment) { id =>
      respondWithMediaType(`application/json`) {
        requestContext =>
          val executionService = actorRefFactory.actorOf(WorkflowExecutionService.props(executionServiceHandler, requestContext))
          executionService ! WorkflowExecutionService.WorkflowStatus(id)
      }
    }
}

object WorkflowExecutionService {
  case class WorkflowStart(workflowExecution: WorkflowExecution)
  case class WorkflowStatus(id: String)

  def props(executionServiceHandler: RequestContext => WorkflowExecutionService, requestContext: RequestContext): Props = {
    Props(executionServiceHandler(requestContext))
  }

}

trait WorkflowExecutionService extends Actor {
  val requestContext: RequestContext

  implicit val system = context.system
  val log = Logging(system, getClass)


  override def receive = {
    case WorkflowExecutionService.WorkflowStart(workflowExecution) =>
      start(workflowExecution)
      context.stop(self)
    case WorkflowExecutionService.WorkflowStatus(id) =>
      status(id)
      context.stop(self)
  }

  /**
   * Starts a workflow execution, emits response directly to requestContext which should include
   * the id of the workflow execution
   */
  def start(workflowExecution: WorkflowExecution)
  
  /**
   * Gets status of a workflow execution, emits response directly to requestContext
   */
  def status(id: String)
}