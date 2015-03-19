/*
Copyright (c) 2015, Broad Institute, Inc.
All rights reserved.
 
Redistribution and use in source and binary forms, with or without
  modification, are permitted provided that the following conditions are met:
 
* Redistributions of source code must retain the above copyright notice, this
  list of conditions and the following disclaimer.
 
* Redistributions in binary form must reproduce the above copyright notice,
  this list of conditions and the following disclaimer in the documentation
  and/or other materials provided with the distribution.
 
* Neither the name Broad Institute, Inc. nor the names of its
  contributors may be used to endorse or promote products derived from
  this software without specific prior written permission.
 
THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
  DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
    SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE
*/

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