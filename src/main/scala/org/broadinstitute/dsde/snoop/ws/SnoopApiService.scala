package org.broadinstitute.dsde.snoop

import akka.actor.{Props, Actor}
import spray.httpx.SprayJsonSupport
import spray.routing._
import spray.json._
import spray.http.MediaTypes._
import spray.httpx.marshalling.ToResponseMarshallable.isMarshallable
import spray.routing.Directive.pimpApply
import org.broadinstitute.dsde.snoop.ws._

class SnoopApiServiceActor extends Actor with SnoopApiService {
  def actorRefFactory = context
  def receive = runRoute(snoopRoute)
}


// this trait defines our service behavior independently from the service actor
trait SnoopApiService extends HttpService {
  implicit def executionContext = actorRefFactory.dispatcher
  import WorkflowExecutionJsonSupport._
  import SprayJsonSupport._

  def snoop2ZamboniWorkflow(exeMessage: WorkflowExecution) : ZamboniSubmission = {
    var zamboniWorkflow = exeMessage.workflowParameters
    zamboniWorkflow += ("gcsSandboxBucket" -> "gs://broad-dsde-dev-public/snoop")
    val zamboniRequest = ZamboniWorkflow(Map("workflow"-> exeMessage.workflowId), zamboniWorkflow)
    val zamboniRequestString = zamboniRequest.toJson.toString
    val zamboniMessage = ZamboniSubmission("some-token", zamboniRequestString)
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
        post {
          entity(as[WorkflowExecution]) { workflowExecution =>
            requestContext =>
              val submission = snoop2ZamboniWorkflow(workflowExecution)
              val executionService = actorRefFactory.actorOf(Props(new ExecutionService(requestContext)))
              executionService ! ExecutionService.Process(submission)
          }
        }
      } ~
    path("workflowExecutions" / Segment) { id =>
        respondWithMediaType(`application/json`) {
          complete {
            //placeholder to call the getStatus api
            WorkflowExecution(Option(id), Map.empty, "workflow_id", "callback", Some("running"))
          }
        }
      }
}