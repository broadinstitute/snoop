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

import akka.actor.{ActorSystem, Props}
import akka.io.IO
import org.broadinstitute.dsde.snoop.ws.{StandardZamboniApi, ZamboniWorkflowExecutionService}
import spray.can.Http
import akka.pattern.ask
import akka.util.Timeout
import spray.routing.RequestContext
import scala.concurrent.duration._
import com.typesafe.config.ConfigFactory
import java.io.File

object Boot extends App {

  // we need an ActorSystem to host our application in
  implicit val system = ActorSystem("snoop")
  
  val conf = ConfigFactory.parseFile(new File("/etc/snoop.conf"))

  val executionServiceHandler: RequestContext => WorkflowExecutionService = ZamboniWorkflowExecutionService(StandardZamboniApi(conf.getString("zamboni.server")))

  // create and start our service actor
  val service = system.actorOf(SnoopApiServiceActor.props(executionServiceHandler), "snoop-service")

  implicit val timeout = Timeout(5.seconds)
  // start a new HTTP server on port 8080 with our service actor as the handler
  IO(Http) ? Http.Bind(service, interface = "0.0.0.0", port = 8080)
}