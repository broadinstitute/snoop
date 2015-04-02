package org.broadinstitute.dsde.snoop

import akka.actor.{ActorRefFactory, Props, Actor}
import com.typesafe.config.Config
import org.broadinstitute.dsde.snoop.dataaccess.SnoopSubmissionController
import org.broadinstitute.dsde.snoop.ws.PerRequest.RequestComplete
import spray.http.StatusCodes
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
  def props(executionServiceConstructor: () => WorkflowExecutionService, swaggerService: SwaggerService): Props = {
    Props(new SnoopApiServiceActor(executionServiceConstructor, swaggerService))
  }
}

class SwaggerService(override val apiVersion: String,
                     override val baseUrl: String,
                     override val docsPath: String,
                     override val swaggerVersion: String,
                     override val apiTypes: Seq[Type],
                     override val apiInfo: Option[ApiInfo])(implicit val actorRefFactory: ActorRefFactory) extends SwaggerHttpService

class SnoopApiServiceActor(executionServiceCtor: () => WorkflowExecutionService, swaggerService: SwaggerService) extends Actor with RootSnoopApiService with WorkflowExecutionApiService {
  implicit def executionContext = actorRefFactory.dispatcher
  def actorRefFactory = context
  def possibleRoutes = baseRoute ~ workflowRoutes ~ swaggerService.routes 

  def receive = runRoute(possibleRoutes)
  def apiTypes = Seq(typeOf[RootSnoopApiService], typeOf[WorkflowExecutionApiService])
  def executionServiceConstructor(): WorkflowExecutionService = executionServiceCtor()
}

@Api(value = "", description = "Snoop Base API", position = 1)
trait RootSnoopApiService extends HttpService {
  def executionServiceConstructor(): WorkflowExecutionService

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
trait WorkflowExecutionApiService extends HttpService with PerRequestCreator {
  import WorkflowExecutionJsonSupport._
  import SprayJsonSupport._

  def executionServiceConstructor(): WorkflowExecutionService

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
              perRequest(requestContext, WorkflowExecutionService.props(executionServiceConstructor), WorkflowExecutionService.WorkflowStart(workflowExecution, securityToken))
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
            perRequest(requestContext, WorkflowExecutionService.props(executionServiceConstructor), WorkflowExecutionService.WorkflowStatus(id, securityToken))
        }
      }
    }
}

object WorkflowExecutionService {
  case class WorkflowStart(workflowExecution: WorkflowExecution, securityToken: String)
  case class WorkflowStatus(id: String, securityToken: String)

  def props(executionServiceConstructor: () => WorkflowExecutionService): Props = {
    Props(executionServiceConstructor())
  }

}

trait WorkflowExecutionService extends Actor {
  val snoopSubmissionController: SnoopSubmissionController

  implicit val system = context.system
  val log = Logging(system, getClass)


  override def receive = {
    case WorkflowExecutionService.WorkflowStart(workflowExecution, securityToken) =>
      val response = start(workflowExecution, securityToken)
      snoopSubmissionController.createSubmission(workflowExecution.workflowId, workflowExecution.callbackUri, response.status.getOrElse(throw new SnoopException("status missing in response")))
      context.parent ! RequestComplete(StatusCodes.Created, response)

    case WorkflowExecutionService.WorkflowStatus(id, securityToken) =>
      val response = status(id, securityToken)
      context.parent ! response
  }

  /**
   * Starts a workflow execution, emits response directly to requestContext which should include
   * the id of the workflow execution
   */
  def start(workflowExecution: WorkflowExecution, securityToken: String): WorkflowExecution
  
  /**
   * Gets status of a workflow execution, emits response directly to requestContext
   */
  def status(id: String, securityToken: String): WorkflowExecution
}
