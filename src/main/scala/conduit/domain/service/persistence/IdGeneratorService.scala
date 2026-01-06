package conduit.domain.service.persistence

import conduit.domain.model.Article
import kyo.*

import java.text.Normalizer
import java.util.UUID

/**
 * Service for generating unique identifiers and URL-friendly slugs.
 *
 * This service provides utilities for creating UUIDs and generating SEO-friendly
 * slugs from article titles. Slugs are URL-safe strings that combine normalized
 * titles with unique identifiers to ensure uniqueness while maintaining readability.
 */
object IdGeneratorService {
  /**
   * Generates a new random UUID.
   *
   * @return a new UUID wrapped in a Sync effect
   */
  def generate: UUID < Sync =
    Sync.defer(UUID.randomUUID())

  /**
   * Creates a URL-friendly slug from an article title.
   *
   * The slug combines a normalized version of the title with a UUID to ensure
   * uniqueness while maintaining readability. For example, "Hello World!" might
   * become "hello-world-a1b2c3d4-e5f6-7890-abcd-ef1234567890".
   *
   * @param title the article title to convert into a slug
   * @return a unique URL-friendly slug
   */
  def slug(title: String): String < Sync =
    for {
      normalized <- normalize(title)
      uuid       <- generate
    } yield s"$normalized-$uuid"

  /**
   * Normalizes a title string into a URL-friendly format.
   *
   * The normalization process:
   * 1. Trims and converts to lowercase
   * 2. Removes Unicode diacritical marks (accents)
   * 3. Replaces non-alphanumeric characters with hyphens
   * 4. Removes leading and trailing hyphens
   *
   * @param title the title string to normalize
   * @return a normalized, URL-friendly string
   */
  def normalize(title: String): String < Any =
    Normalizer
      .normalize(title.trim.toLowerCase, Normalizer.Form.NFD)
      .replaceAll("\\p{M}", "")
      .replaceAll("[^a-z0-9]+", "-")
      .replaceAll("(^-|-$)", "")
}
