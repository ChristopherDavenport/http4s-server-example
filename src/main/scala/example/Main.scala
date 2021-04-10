package example

import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits._
// import com.twitter.finagle.{Http => FHttp}
// import com.twitter.util.Await
import org.http4s._
import org.http4s.implicits._
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.blaze.BlazeServerBuilder
// import org.http4s.server.jetty.JettyBuilder
// import org.http4s.netty.server.NettyServerBuilder
// import org.http4s.server.tomcat.TomcatBuilder
// import org.http4s.finagle._
import scala.concurrent.duration._

object Main {
  import org.http4s.dsl.io._
  // private val action = IO.pure(Response[IO](Status.Ok))
  val sarray = Array.fill(10000)(1.toByte)
  val barray = Array.fill(10000000)(1.toByte)
  val app = HttpRoutes
    .of[IO] {
      case req@ GET -> Root / "foo" => 
        Ok()
        // (req.body.compile.drain, action).mapN{ case (_, resp) => resp}
      // case req if req.method == Method.GET && req.pathInfo.renderString == "/smallArray" => 
      //   IO(Response().withEntity(sarray))
      // case req if req.method == Method.GET && req.pathInfo.renderString == "/bigArray" => 
      //   IO(Response().withEntity(barray))
      // case req if req.method == Method.GET && req.pathInfo.renderString == "/hello" =>
      //   IO(Response(Status.Ok).withEntity("Hello World in " + Thread.currentThread().getName))
    }
    .orNotFound
}

// object NettyTestServer extends IOApp {
//   override def run(args: List[String]): IO[ExitCode] =
//     NettyServerBuilder[IO].withHttpApp(Main.app).resource.use(_ => IO.never)
// }

// object BlazeTestServer extends IOApp {
//   override def run(args: List[String]): IO[ExitCode] =
//     BlazeServerBuilder[IO](concurrent.ExecutionContext.global)
//       .withHttpApp(Main.app)
//       .resource
//       .use(_ => IO.never)

// }

object EmberTestServer extends IOApp {
  val logger = io.chrisdavenport.log4cats.slf4j.Slf4jLogger.getLogger[IO]
  override def run(args: List[String]): IO[ExitCode] =
    EmberServerBuilder
      .default[IO]
      .withHost("0.0.0.0")
      .withPort(8081)
      .withMaxConcurrency(1024) //tweak it
      .withHttpApp(Main.app)
      .withLogger(logger)
      .withErrorHandler({
        case t => logger.error(t)("Unexpected service error").as(Response[IO](Status.InternalServerError))
      })
      // .withOnWriteFailure{ case (req, resp, t) => logger.info(t)(s"Failed to write Response: $resp for req: $req")}
      .build
      .use(_ => IO.never)
}

object EmberClientApp extends IOApp {
  import org.http4s.implicits._
  def run(args: List[String]): IO[ExitCode] = 
    org.http4s.ember.client.EmberClientBuilder.default[IO].build.use{client => 
      client.run(Request[IO](Method.GET, uri"http://localhost:8081/foo")).use{//.putHeaders(org.http4s.headers.`Content-Length`.zero)).use{
        resp => 
        resp.body.compile.drain
      }
        .as(ExitCode.Success)
    }
}

// object JettyTestServer extends IOApp {
//   override def run(args: List[String]): IO[ExitCode] =
//     JettyBuilder[IO]
//       .bindHttp(8080)
//       .mountHttpApp(Main.app, "/")
//       .resource
//       .use(_ => IO.never)
// }

// object TomcatTestServer extends IOApp {
//   override def run(args: List[String]): IO[ExitCode] =
//     TomcatBuilder[IO]
//       .bindHttp(8080)
//       .mountHttpApp(Main.app, "/")
//       .resource
//       .use(_ => IO.never)
// }

// object FinagleTestServer extends IOApp {
//   override def run(args: List[String]): IO[ExitCode] = {
//     val server = IO(FHttp.server.serve(":8080", Finagle.mkService(Main.app)))
//     server.map(Await.ready(_)) >> IO.never
//   }
// }
