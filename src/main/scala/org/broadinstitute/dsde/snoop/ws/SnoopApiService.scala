package org.broadinstitute.dsde.snoop

import akka.actor.{Props, Actor}
import spray.httpx.SprayJsonSupport
import spray.routing._
import spray.json._
import spray.http.MediaTypes._
import spray.httpx.marshalling.ToResponseMarshallable.isMarshallable
import spray.routing.Directive.pimpApply
import org.broadinstitute.dsde.snoop.ws._
import akka.event.Logging

class SnoopApiServiceActor(zamboniServer: String) extends Actor with SnoopApiService {
  def actorRefFactory = context
  def receive = runRoute(snoopRoute)
  val zamboniApi = new ZamboniApiImpl(zamboniServer)(context.system)
}


// this trait defines our service behavior independently from the service actor
trait SnoopApiService extends HttpService {
  implicit def executionContext = actorRefFactory.dispatcher
  import WorkflowExecutionJsonSupport._
  import SprayJsonSupport._

  val zamboniApi: ZamboniApi
  
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
            val executionService = actorRefFactory.actorOf(Props(ZamboniWorkflowExecutionService(requestContext, zamboniApi)))
            executionService ! WorkflowStart(workflowExecution)
        }
      }
    } ~
    path("workflowExecutions" / Segment) { id =>
      respondWithMediaType(`application/json`) {
        requestContext =>
          val executionService = actorRefFactory.actorOf(Props(ZamboniWorkflowExecutionService(requestContext, zamboniApi)))
          executionService ! WorkflowStatus(id)
      }
    }
}

case class WorkflowStart(workflowExecution: WorkflowExecution)
case class WorkflowStatus(id: String)

trait WorkflowExecutionService extends Actor {
  val requestContext: RequestContext

  implicit val system = context.system
  import system.dispatcher
  val log = Logging(system, getClass)


  override def receive = {
    case WorkflowStart(workflowExecution) => start(workflowExecution)
    case WorkflowStatus(id) => status(id)
      
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