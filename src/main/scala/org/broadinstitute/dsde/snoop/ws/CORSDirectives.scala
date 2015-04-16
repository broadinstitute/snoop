package org.broadinstitute.dsde.snoop.ws

import java.io.File

import com.typesafe.config.ConfigFactory
import org.broadinstitute.dsde.snoop.SwaggerService
import spray.http.HttpHeaders._
import spray.http.HttpMethods._
import spray.http._
import spray.routing._
/**
 * https://gist.github.com/joseraya/176821d856b43b1cfe19
 * https://github.com/giftig/mediaman/blob/22b95a807f6e7bb64d695583f4b856588c223fc1/src/main/scala/com/programmingcentre/utils/utils/CorsSupport.scala
 */

trait CorsDirectives {
  this: HttpService =>

  //val conf = ConfigFactory.parseFile(new File("/etc/snoop.conf"))
  //val allowOriginHost = conf.getConfig("swagger").getString("origin")
  private val allowHeaders = `Access-Control-Allow-Headers`("Origin, X-Requested-With,X-Auth-Token, Content-Type, Accept, Accept-Encoding, Accept-Language, Host, Referer, User-Agent, Set-Cookie")
  private val optionsCorsHeaders = List(
    allowHeaders,
    `Access-Control-Max-Age`(60 * 60 * 24 * 20))  // cache pre-flight response for 20 days)
  private val allowCredentialsHeader = `Access-Control-Allow-Credentials`(true)

  def cors[T](swaggerOrgin: String) : Directive0 = {
    val allowOriginHeader = `Access-Control-Allow-Origin`(SomeOrigins(Vector(swaggerOrgin)))

    mapRequestContext { context => context.withRouteResponseHandling({
      // If an OPTIONS request was rejected as 405, complete the request by responding with the
      // defined CORS details and the allowed options grabbed from the rejection
      case Rejected(reasons) if (
        context.request.method == HttpMethods.OPTIONS &&
          reasons.exists(_.isInstanceOf[MethodRejection])
        ) => {
        val allowedMethods = reasons.collect{ case r: MethodRejection => r.supported}
        context.complete(HttpResponse().withHeaders(
          `Access-Control-Allow-Methods`(OPTIONS, allowedMethods: _*) ::
            allowOriginHeader ::
            optionsCorsHeaders
        ))
      }
    }).withHttpResponseHeadersMapped {
      //added allowHeaders here to avoid error "Request header field Content-Type is not allowed by Access-Control-Allow-Headers"
      headers => allowOriginHeader :: allowHeaders :: allowCredentialsHeader :: headers
    }
    }
  }
}
