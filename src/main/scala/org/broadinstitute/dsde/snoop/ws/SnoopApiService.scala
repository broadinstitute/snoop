package org.broadinstitute.dsde.snoop

import akka.actor.Actor
import spray.routing._
import spray.http._
import spray.http.MediaTypes._
import spray.httpx.marshalling.ToResponseMarshallable.isMarshallable
import spray.routing.Directive.pimpApply
import org.broadinstitute.dsde.snoop.ws.WorkflowExecution
import org.broadinstitute.dsde.snoop.ws.WorkflowExecutionJsonSupport._


class SnoopApiServiceActor extends Actor with SnoopApiService {
  def actorRefFactory = context
  def receive = runRoute(snoopRoute)
}


// this trait defines our service behavior independently from the service actor
trait SnoopApiService extends HttpService {

  val snoopRoute =
    path("workflowExecutions") {
      post {
        entity(as[WorkflowExecution]) { workflowExecution =>
          respondWithMediaType(`application/json`) {
            complete {
              workflowExecution.copy(id=Some("static_id"))
            }
          }
        }
      }
    } ~
    path("workflowExecutions" / Segment) { id =>
          respondWithMediaType(`application/json`) {
            complete {
              WorkflowExecution(Some(id), Map.empty, None, "workflow_id", "callback")
            }
          }
      }
}