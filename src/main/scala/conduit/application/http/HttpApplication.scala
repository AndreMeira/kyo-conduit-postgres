package conduit.application.http

import conduit.domain.error.ApplicationError
import conduit.domain.service.authentication.{ AuthenticationService, Module as AuthenticationModule }
import conduit.domain.service.usecase.Module as UseCaseModule
import conduit.domain.service.validation.Module as ValidationModule
import conduit.infrastructure.configuration.Module as ConfigurationModule
import conduit.infrastructure.postgres.{ Migration, PostgresTransaction, Module as PostgresModule }
import kyo.*

/** Entry point for the Conduit HTTP application.
  *
  * Composes all layers (Postgres infrastructure, authentication, validation,
  * use cases, HTTP routes) and starts a kyo-http server exposing the Conduit API.
  */
object HttpApplication extends KyoApp:

  case class HttpServerError(message: String) extends ApplicationError

  /**
   * The main entry point of the application. It runs the application next and handles any
   * errors that may occur during startup. If an error occurs, it will be mapped to a
   * RuntimeException with the error message, which will cause the application to fail
   * with a clear error message.
   */
  run {
    application
  }

  /**
   * The main application next that composes the necessary layers and starts the server.
   * It retrieves the HTTP routes from the application layer and runs the Netty server
   * with those routes. The server will handle incoming HTTP requests according to the
   * defined API endpoints.
   */
  private def application: Unit < (Scope & Abort[ApplicationError] & Async) =
    Memo.run {
      Env.runLayer(applicationLayer):
        for
          migration  <- Env.get[Migration]
          httpRoutes <- Env.get[HttpRoutes]
          waiting    <- Promise.init[Unit, Any]
          _          <- migration.applyMigrations
          server     <- HttpServer
                          .init(8080, "0.0.0.0")(httpRoutes.all*)
                          .mapAbort(error => HttpServerError(error.getMessage))
          _          <- waiting.get
        yield ()
    }

  /**
   * Composes all the necessary layers for the application, including configuration,
   * Postgres infrastructure, authentication service, validation service, use cases,
   * and HTTP routes. This layer provides the HttpRoutes service that will be used
   * to handle incoming HTTP requests.
   */
  private val applicationLayer = Layer.init[HttpRoutes & Migration](
    // Infrastructure
    ConfigurationModule.all,
    PostgresModule.all,

    // Domain services
    AuthenticationModule.authenticationService,
    ValidationModule.stateValidationService[PostgresTransaction],

    // Use cases
    UseCaseModule.useCases[PostgresTransaction],

    // HTTP
    Module.httpRoutes[PostgresTransaction],
  )
