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

package org.broadinstitute.dsde.snoop.ws

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
  def apply(zamboniApi: ZamboniApi)(requestContext: RequestContext): ZamboniWorkflowExecutionService = new ZamboniWorkflowExecutionService(requestContext, zamboniApi)
}

case class ZamboniWorkflowExecutionService(requestContext: RequestContext, zamboniApi: ZamboniApi) extends WorkflowExecutionService {
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
    val zamboniRequest = ZamboniWorkflow(Map("workflow"-> exeMessage.workflowId), exeMessage.workflowParameters + ("gcsSandboxBucket" -> "gs://broad-dsde-dev-public/snoop"))
    val zamboniRequestString = zamboniRequest.toJson.toString
    val zamboniMessage = ZamboniSubmission("some-token", zamboniRequestString)
    zamboniMessage
  }

}