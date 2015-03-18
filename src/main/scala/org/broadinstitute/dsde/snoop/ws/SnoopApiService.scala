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
  def props(executionServicePropsFunc: RequestContext => Props): Props = Props(new SnoopApiServiceActor(executionServicePropsFunc))
}

class SnoopApiServiceActor(executionServicePropsFunc: RequestContext => Props) extends Actor with SnoopApiService {
  def actorRefFactory = context
  def receive = runRoute(snoopRoute)

  override val executionServiceProps = executionServicePropsFunc
}


// this trait defines our service behavior independently from the service actor
trait SnoopApiService extends HttpService {
  implicit def executionContext = actorRefFactory.dispatcher
  import WorkflowExecutionJsonSupport._
  import SprayJsonSupport._

  def executionServiceProps: RequestContext => Props

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
            val executionService = actorRefFactory.actorOf(executionServiceProps(requestContext))
            executionService ! WorkflowStart(workflowExecution)
        }
      }
    } ~
    path("workflowExecutions" / Segment) { id =>
      respondWithMediaType(`application/json`) {
        requestContext =>
          val executionService = actorRefFactory.actorOf(executionServiceProps(requestContext))
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