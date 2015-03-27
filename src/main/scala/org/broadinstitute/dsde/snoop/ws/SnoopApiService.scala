package org.broadinstitute.dsde.snoop

import akka.actor.{ActorRefFactory, Props, Actor}
import com.typesafe.config.Config
import spray.httpx.SprayJsonSupport
import spray.routing._
import spray.json._
import spray.http.MediaTypes._
import spray.httpx.marshalling.ToResponseMarshallable.isMarshallable
import spray.routing.Directive.pimpApply
import org.broadinstitute.dsde.snoop.ws._
import akka.event.Logging
import com.wordnik.swagger.annotations._
import com.wordnik.swagger.model.ApiInfo
import scala.reflect.runtime.universe._
import com.gettyimages.spray.swagger.SwaggerHttpService
import java.io.File
import com.typesafe.config.{Config, ConfigFactory}

object SnoopApiServiceActor {
  def props(executionServiceHandler: RequestContext => WorkflowExecutionService, swaggerService: SwaggerService): Props = {
    Props(new SnoopApiServiceActor(executionServiceHandler, swaggerService))
  }
}

class SwaggerService(override val apiVersion: String,
                     override val baseUrl: String,
                     override val docsPath: String,
                     override val swaggerVersion: String,
                     override val apiTypes: Seq[Type],
                     override val apiInfo: Option[ApiInfo])(implicit val actorRefFactory: ActorRefFactory) extends SwaggerHttpService

class SnoopApiServiceActor(override val executionServiceHandler: RequestContext => WorkflowExecutionService, swaggerService: SwaggerService) extends Actor with RootSnoopApiService with WorkflowExecutionApiService {
  implicit def executionContext = actorRefFactory.dispatcher
  def actorRefFactory = context
  def possibleRoutes = baseRoute ~ workflowRoutes ~ swaggerService.routes 

  def receive = runRoute(possibleRoutes)
  def apiTypes = Seq(typeOf[RootSnoopApiService], typeOf[WorkflowExecutionApiService])
}

@Api(value = "", description = "Snoop Base API", position = 1)
trait RootSnoopApiService extends HttpService {
  def executionServiceHandler: RequestContext => WorkflowExecutionService

  @ApiOperation(value = "Check if Snoop is alive",
    nickname = "poke",
    httpMethod = "GET",
    produces = "text/html",
    response = classOf[String])
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Successful Request"),
    new ApiResponse(code = 500, message = "Snoop Internal Error")
  ))  
   def baseRoute =
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
    path("headers") {
      get {
        requestContext => requestContext.complete(requestContext.request.headers.mkString(",\n"))
      }
    }
}

// this trait defines our service behavior independently from the service actor
@Api(value = "workflowExecutions", description = "Snoop Workflow Execution API", position = 1)
trait WorkflowExecutionApiService extends HttpService {
  import WorkflowExecutionJsonSupport._
  import SprayJsonSupport._

  def executionServiceHandler: RequestContext => WorkflowExecutionService

  def workflowRoutes = startWorkflowRoute ~ workflowStatusRoute

  @ApiOperation(value = "Start workflow",
    nickname = "start workflow",
    httpMethod = "POST",
    produces = "text/html",
    response = classOf[WorkflowExecution])
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Successful Request"),
    new ApiResponse(code = 500, message = "Snoop Internal Error")
  ))  
  def startWorkflowRoute = 
    cookie("iPlanetDirectoryPro") { securityTokenCookie =>
      val securityToken = securityTokenCookie.content
      path("workflowExecutions") {
        post {
          entity(as[WorkflowExecution]) { workflowExecution =>
            requestContext =>
              val executionService = actorRefFactory.actorOf(WorkflowExecutionService.props(executionServiceHandler, requestContext))
              executionService ! WorkflowExecutionService.WorkflowStart(workflowExecution, securityToken)
          }
        }
      }
    }

  @ApiOperation(value = "Get workflow status",
    nickname = "workflow status",
    httpMethod = "GET",
    produces = "application/json",
    response = classOf[WorkflowExecution])
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "id", required = true, dataType = "string", paramType = "path", value = "workflow id")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "successful request"),
    new ApiResponse(code = 404, message = "workflow id not found"),
    new ApiResponse(code = 500, message = "Snoop internal error")
  ))  
  def workflowStatusRoute =
    cookie("iPlanetDirectoryPro") { securityTokenCookie =>
      val securityToken = securityTokenCookie.content
      path("workflowExecutions" / Segment) { id =>
        respondWithMediaType(`application/json`) {
          requestContext =>
            val executionService = actorRefFactory.actorOf(WorkflowExecutionService.props(executionServiceHandler, requestContext))
            executionService ! WorkflowExecutionService.WorkflowStatus(id, securityToken)
        }
      }
    }
}

object WorkflowExecutionService {
  case class WorkflowStart(workflowExecution: WorkflowExecution, securityToken: String)
  case class WorkflowStatus(id: String, securityToken: String)

  def props(executionServiceHandler: RequestContext => WorkflowExecutionService, requestContext: RequestContext): Props = {
    Props(executionServiceHandler(requestContext))
  }

}

trait WorkflowExecutionService extends Actor {
  val requestContext: RequestContext

  implicit val system = context.system
  val log = Logging(system, getClass)


  override def receive = {
    case WorkflowExecutionService.WorkflowStart(workflowExecution, securityToken) =>
      start(workflowExecution, securityToken)
      context.stop(self)
    case WorkflowExecutionService.WorkflowStatus(id, securityToken) =>
      status(id, securityToken)
      context.stop(self)
  }

  /**
   * Starts a workflow execution, emits response directly to requestContext which should include
   * the id of the workflow execution
   */
  def start(workflowExecution: WorkflowExecution, securityToken: String)
  
  /**
   * Gets status of a workflow execution, emits response directly to requestContext
   */
  def status(id: String, securityToken: String)
}
