package org.broadinstitute.dsde.snoop

import akka.actor.{Props, Actor}
import akka.event.Logging
import com.lambdaworks.jacks.JacksMapper
import spray.httpx.SprayJsonSupport
import spray.routing._
import spray.http._
import spray.http.MediaTypes._
import spray.httpx.marshalling.ToResponseMarshallable.isMarshallable
import spray.routing.Directive.pimpApply
import org.broadinstitute.dsde.snoop.ws._

class SnoopApiServiceActor extends Actor with SnoopApiService  {
  def actorRefFactory = context
  def receive = runRoute(snoopRoute)
}


// this trait defines our service behavior independently from the service actor
trait SnoopApiService extends HttpService {
  implicit def executionContext = actorRefFactory.dispatcher
  import WorkflowExecutionJsonSupport._
  import SprayJsonSupport._

  def workflowExecution2ZamboniWorkflow(exeMessage: WorkflowExecution) : ZamboniSubmission = {
    val zamboniRequest = ZamboniWorkflow(Map("Zamboni"-> exeMessage.workflowId),exeMessage.workflowParameters)
    val zamboniRequestString = JacksMapper.writeValueAsString[ZamboniWorkflow](zamboniRequest)
    val zamboniMessage = ZamboniSubmission(exeMessage.id.getOrElse("some_id"), "some-token", zamboniRequestString)
    zamboniMessage
  }

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
        entity(as[WorkflowExecution]) { workflowExecution =>
          requestContext =>
            val submission = workflowExecution2ZamboniWorkflow(workflowExecution)
            val executionService = actorRefFactory.actorOf(Props(new ExecutionService(requestContext)))
            executionService ! ExecutionService.Process(submission)
        }
      } ~
    path("workflowExecutions" / Segment) { id =>
        respondWithMediaType(`application/json`) {
          complete {
            WorkflowExecution(Some(id), Map.empty, "workflow_id", "callback", Some("running"))
          }
        }
      }
}