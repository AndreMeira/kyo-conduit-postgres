package conduit.infrastructure.inmemory

import com.andremeira.test.KyoTestSuite
import com.andremeira.test.KyoTestSuite.SuiteResult
import conduit.domain.model.Credentials
import conduit.domain.service.persistence.{ IdGeneratorService, Persistence }
import conduit.infrastructure.inmemory.InMemoryTestSupport.withDatabase
import kyo.*

object InMemoryCredentialsRepositorySpec extends KyoTestSuite:

  def makePersistence: Persistence[InMemoryTransaction] < (Sync & Scope) =
    InMemoryTestSupport.makePersistence

  def specSuite: SuiteResult < (Async & Scope) =

    "InMemoryCredentialsRepository" should {
      "save and find by user ID" in withDatabase { database =>
        database.transaction:
          for
            persistence              <- makePersistence
            userId                   <- IdGeneratorService.uuid
            creds: Credentials.Hashed = Credentials.Hashed(s"$userId@test.com", "hashed_password")
            _                        <- persistence.credentials.save(userId, creds)
            found                    <- persistence.credentials.find(userId)
          yield assert(found == Maybe.Present(creds), s"Expected $creds but got $found")
      }

      "save and find by hashed credentials" in withDatabase { database =>
        database.transaction:
          for
            persistence              <- makePersistence
            userId                   <- IdGeneratorService.uuid
            creds: Credentials.Hashed = Credentials.Hashed(s"$userId@test.com", "hashed_password")
            _                        <- persistence.credentials.save(userId, creds)
            found                    <- persistence.credentials.find(creds)
          yield assert(found == Maybe.Present(userId), s"Expected $userId but got $found")
      }

      "return Emtpy for an unknown user ID" in withDatabase { database =>
        database.transaction:
          for
            persistence <- makePersistence
            unknownId   <- IdGeneratorService.uuid
            found       <- persistence.credentials.find(unknownId)
          yield assert(found == Maybe.Absent, s"Expected Emtpy but got $found")
      }

      "return Emtpy for credentials that do not match" in withDatabase { database =>
        database.transaction:
          for
            persistence              <- makePersistence
            userId                   <- IdGeneratorService.uuid
            creds: Credentials.Hashed = Credentials.Hashed(s"$userId@test.com", "correct_hash")
            wrong: Credentials.Hashed = Credentials.Hashed(s"$userId@test.com", "wrong_hash")
            _                        <- persistence.credentials.save(userId, creds)
            found                    <- persistence.credentials.find(wrong)
          yield assert(found == Maybe.Absent, s"Expected Emtpy for wrong password but got $found")
      }

      "report email existence before and after save" in withDatabase { database =>
        database.transaction:
          for
            persistence              <- makePersistence
            userId                   <- IdGeneratorService.uuid
            email                     = s"$userId@test.com"
            creds: Credentials.Hashed = Credentials.Hashed(email, "hashed_password")
            beforeSave               <- persistence.credentials.exists(email)
            _                        <- persistence.credentials.save(userId, creds)
            afterSave                <- persistence.credentials.exists(email)
          yield assert(!beforeSave, "email should not exist before save") &
            assert(afterSave, "email should exist after save")
      }

      "update credentials" in withDatabase { database =>
        database.transaction:
          for
            persistence                 <- makePersistence
            userId                      <- IdGeneratorService.uuid
            original: Credentials.Hashed = Credentials.Hashed(s"$userId@test.com", "original_hash")
            updated: Credentials.Hashed  = Credentials.Hashed(s"$userId@test.com", "updated_hash")
            _                           <- persistence.credentials.save(userId, original)
            _                           <- persistence.credentials.update(userId, updated)
            found                       <- persistence.credentials.find(userId)
          yield assert(found == Maybe.Present(updated), s"Expected updated creds but got $found")
      }

      "delete credentials" in withDatabase { database =>
        database.transaction:
          for
            persistence              <- makePersistence
            userId                   <- IdGeneratorService.uuid
            creds: Credentials.Hashed = Credentials.Hashed(s"$userId@test.com", "hashed_password")
            _                        <- persistence.credentials.save(userId, creds)
            _                        <- persistence.credentials.delete(userId)
            found                    <- persistence.credentials.find(userId)
          yield assert(found == Maybe.Absent, s"Expected Emtpy after delete but got $found")
      }
    }
