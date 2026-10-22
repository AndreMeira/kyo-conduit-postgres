package conduit.infrastructure.postgres

import com.andremeira.test.KyoTestSuite
import com.andremeira.test.KyoTestSuite.SuiteResult
import conduit.domain.model.Credentials
import conduit.domain.service.persistence.{ Database, IdGeneratorService, Persistence }
import conduit.domain.types.*
import conduit.infrastructure.postgres.configuration.{ DatabaseConfig, DatasourceConfig, MigrationConfig }
import kyo.*
import org.testcontainers.containers.PostgreSQLContainer

object ModuleSpec extends KyoTestSuite:
  import Support.*

  def specSuite: SuiteResult < (Async & Scope) =
    "Postgres Module" should PostgresTestSupport.withContainer { container =>

      "provide a migratable database whose repositories persist across transactions" in
        Memo.run:
          Env.runLayer(Layer(databaseConfig(container)), Module.all):
            for
              migration   <- Env.get[Migration]
              database    <- Env.get[Database[PostgresTransaction]]
              persistence <- Env.get[Persistence[PostgresTransaction]]
              _           <- migration.applyMigrations
              userId      <- IdGeneratorService.uuid.map(UserId(_))
              email        = Email(s"$userId@test.com")
              _           <- database.transaction(persistence.credentials.save(userId, Credentials.Hashed(email, Password("hashed"))))
              saved       <- database.transaction(persistence.credentials.exists(email))
            yield assert(saved, s"Expected $email to be saved")
    }

  private object Support:

    /**
     * The configuration of the container's database, as the application would load it.
     *
     * @param container the running container
     * @return the configuration
     */
    def databaseConfig(container: PostgreSQLContainer[?]): DatabaseConfig =
      DatabaseConfig(
        datasource = DatasourceConfig(
          user = container.getUsername,
          password = container.getPassword,
          poolSize = 2,
          database = container.getDatabaseName,
          host = container.getHost,
          port = container.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT),
        ),
        migration = MigrationConfig(initSql = "SET search_path TO public", locations = List("classpath:migrations/schema")),
      )
