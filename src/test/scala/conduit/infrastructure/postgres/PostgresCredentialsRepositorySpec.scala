package conduit.infrastructure.postgres

import com.andremeira.test.KyoTestSuite
import com.andremeira.test.KyoTestSuite.SuiteResult
import conduit.domain.model.Credentials
import conduit.domain.service.persistence.IdGeneratorService
import PostgresTestSupport.withDatabase
import kyo.*

object PostgresCredentialsRepositorySpec extends KyoTestSuite:

  private val persistence = PostgresTestSupport.makePersistence

  def specSuite: SuiteResult < (Async & Scope) =
    "PostgresCredentialsRepository" should withDatabase { database =>

      "save and find by user ID" in
        database.withMigration:
          database.transaction:
            for
              userId                   <- IdGeneratorService.uuid
              creds: Credentials.Hashed = Credentials.Hashed(s"$userId@test.com", "hashed_password")
              _                        <- persistence.credentials.save(userId, creds)
              found                    <- persistence.credentials.find(userId)
            yield assert(found == Maybe.Present(creds), s"Expected $creds but got $found")

      "save and find by hashed credentials" in
        database.withMigration:
          database.transaction:
            for
              userId                   <- IdGeneratorService.uuid
              creds: Credentials.Hashed = Credentials.Hashed(s"$userId@test.com", "hashed_password")
              _                        <- persistence.credentials.save(userId, creds)
              found                    <- persistence.credentials.find(creds)
            yield assert(found == Maybe.Present(userId), s"Expected $userId but got $found")

      "return Emtpy for an unknown user ID" in
        database.withMigration:
          database.transaction:
            for
              unknownId <- IdGeneratorService.uuid
              found     <- persistence.credentials.find(unknownId)
            yield assert(found == Maybe.Absent, s"Expected Emtpy but got $found")

      "return Emtpy for credentials that do not match" in
        database.withMigration:
          database.transaction:
            for
              userId                   <- IdGeneratorService.uuid
              creds: Credentials.Hashed = Credentials.Hashed(s"$userId@test.com", "correct_hash")
              wrong: Credentials.Hashed = Credentials.Hashed(s"$userId@test.com", "wrong_hash")
              _                        <- persistence.credentials.save(userId, creds)
              found                    <- persistence.credentials.find(wrong)
            yield assert(found == Maybe.Absent, s"Expected Emtpy for wrong password but got $found")

      "report email existence before and after save" in
        database.withMigration:
          database.transaction:
            for
              userId                   <- IdGeneratorService.uuid
              email                     = s"$userId@test.com"
              creds: Credentials.Hashed = Credentials.Hashed(email, "hashed_password")
              beforeSave               <- persistence.credentials.exists(email)
              _                        <- persistence.credentials.save(userId, creds)
              afterSave                <- persistence.credentials.exists(email)
            yield assert(!beforeSave, "email should not exist before save") &
              assert(afterSave, "email should exist after save")

      "update credentials" in
        database.withMigration:
          database.transaction:
            for
              userId                      <- IdGeneratorService.uuid
              original: Credentials.Hashed = Credentials.Hashed(s"$userId@test.com", "original_hash")
              updated: Credentials.Hashed  = Credentials.Hashed(s"$userId@test.com", "updated_hash")
              _                           <- persistence.credentials.save(userId, original)
              _                           <- persistence.credentials.update(userId, updated)
              found                       <- persistence.credentials.find(userId)
            yield assert(found == Maybe.Present(updated), s"Expected updated creds but got $found")

      "delete credentials" in
        database.withMigration:
          database.transaction:
            for
              userId                   <- IdGeneratorService.uuid
              creds: Credentials.Hashed = Credentials.Hashed(s"$userId@test.com", "hashed_password")
              _                        <- persistence.credentials.save(userId, creds)
              _                        <- persistence.credentials.delete(userId)
              found                    <- persistence.credentials.find(userId)
            yield assert(found == Maybe.Absent, s"Expected Emtpy after delete but got $found")
    }
