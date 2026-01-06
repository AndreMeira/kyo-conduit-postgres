package conduit.domain.service.persistence

import conduit.domain.model.Article
import kyo.*

import java.text.Normalizer
import java.util.UUID

object IdGeneratorService {
  def generate: UUID < Sync = 
    Sync.defer(UUID.randomUUID())

  def slug(title: String): String < Sync =
    for {
      normalized <- normalize(title)
      uuid       <- generate
    } yield s"$normalized-$uuid"

  def normalize(title: String): String < Any =
    Normalizer
      .normalize(title.trim.toLowerCase, Normalizer.Form.NFD)
      .replaceAll("\\p{M}", "")
      .replaceAll("[^a-z0-9]+", "-")
      .replaceAll("(^-|-$)", "")
}
