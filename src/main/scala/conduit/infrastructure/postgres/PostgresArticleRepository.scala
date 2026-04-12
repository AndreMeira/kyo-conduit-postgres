package conduit.infrastructure.postgres

import com.augustnagro.magnum.*
import com.augustnagro.magnum.MagnumInterop.syntax.*
import com.augustnagro.magnum.MagnumInterop.syntax.{ when, concat }
import conduit.domain.model.Article
import conduit.domain.model.Article.Id
import conduit.domain.service.persistence.ArticleRepository
import conduit.infrastructure.codecs.database.DatabaseCodecs.given
import conduit.infrastructure.postgres.PostgresTransaction.Transactional
import kyo.*

class PostgresArticleRepository extends ArticleRepository[PostgresTransaction] {

  /**
   * Finds an article by its unique ID.
   *
   * @param id the article ID to search for
   * @return a Maybe containing the article if found, or None if not found
   */
  override def find(id: Id): Maybe[Article] < Effect =
    Transactional:
      Maybe.fromOption:
        sql"""SELECT
             a.id,
             a.slug, a.title, a.description, a.body, a.author_id,
             (SELECT count(*) FROM favorites f WHERE f.article_id = a.id) as favorite_count,
             ARRAY(SELECT name FROM tags t WHERE t.article_id = a.id) as tags,
             a.created_at, a.updated_at
             FROM articles a
             WHERE a.id = $id"""
          .query[Article]
          .run()
          .headOption

  /**
   * Finds an article by its slug.
   *
   * @param slug the article slug
   * @return a Maybe containing the article if found, or None if not found
   */
  override def findBySlug(slug: String): Maybe[Article] < Effect =
    Transactional:
      Maybe.fromOption:
        sql"""SELECT
             a.id,
             a.slug, a.title, a.description, a.body, a.author_id,
             (SELECT count(*) FROM favorites f WHERE f.article_id = a.id) as favorite_count,
             ARRAY(SELECT name FROM tags t WHERE t.article_id = a.id) as tags,
             a.created_at, a.updated_at
             FROM articles a
             WHERE a.slug = $slug"""
          .query[Article]
          .run()
          .headOption

  /**
   * Checks if an article with the given ID exists in the repository.
   *
   * @param id the article ID to check
   * @return true if the article exists, false otherwise
   */
  override def exists(id: Id): Boolean < Effect =
    Transactional:
      sql"""SELECT 1 FROM articles WHERE id = $id"""
        .query[Option[Int]]
        .run()
        .nonEmpty

  /**
   * Saves a new article to the repository.
   *
   * @param article the article to save
   * @return Unit on successful save
   */
  override def save(article: Article): Unit < Effect =
    Transactional:
      val count = sql"""
          INSERT INTO articles (
            id, slug, title, description, body, author_id, created_at, updated_at
          ) VALUES (
            ${article.id},
            ${article.slug},
            ${article.title},
            ${article.description},
            ${article.body},
            ${article.authorId},
            ${article.createdAt},
            ${article.updatedAt}
          )""".update
        .run()
      require(count == 1, "Failed to insert article")

  /**
   * Updates an existing article in the repository.
   *
   * @param article the article with updated data
   * @return Unit on successful update
   */
  override def update(article: Article): Unit < Effect =
    Transactional:
      val count = sql"""
          UPDATE articles SET
            slug = ${article.slug},
            title = ${article.title},
            description = ${article.description},
            body = ${article.body},
            author_id = ${article.authorId},
            created_at = ${article.createdAt},
            updated_at = ${article.updatedAt}
          WHERE id = ${article.id}""".update
        .run()
      require(count == 1, "Failed to update article")

  /**
   * Searches for articles based on multiple search parameters.
   *
   * Each optional filter is paired with a Boolean flag. When the flag is false
   * the corresponding condition short-circuits to true, avoiding dynamic SQL
   * fragment concatenation.
   *
   * @param params a list of search parameters to apply
   * @return a list of articles matching all search criteria, ordered by creation date descending
   */
  override def search(params: List[ArticleRepository.SearchParam], offset: Int, limit: Int): List[Article] < Effect =
    Transactional:
      sql"""SELECT
         a.id,
         a.slug, a.title, a.description, a.body, a.author_id,
         (SELECT count(*) FROM favorites f WHERE f.article_id = a.id) as favorite_count,
         ARRAY(SELECT name FROM tags t WHERE t.article_id = a.id) as tags,
         a.created_at, a.updated_at
         FROM articles a
         JOIN profiles p ON a.author_id = p.user_id
         WHERE (${params.author.isEmpty} OR p.name = ${params.author.getOrElse("")})
         AND (${params.tag.isEmpty} OR EXISTS (
           SELECT 1 FROM tags t2
           WHERE t2.article_id = a.id
           AND t2.name = ${params.tag.getOrElse("")}
         ))
         AND (${params.favoriteBy.isEmpty} OR EXISTS (
           SELECT 1 FROM favorites fav
           JOIN profiles fp ON fav.user_id = fp.user_id
           WHERE fav.article_id = a.id
           AND fp.name = ${params.favoriteBy.getOrElse("")}
         ))
         ORDER BY a.created_at DESC LIMIT $limit OFFSET $offset"""
        .query[Article]
        .run()
        .toList

  /**
   * Counts the total number of articles matching the given search parameters.
   *
   * Uses the same filtering semantics as [[searchBy]], ignoring pagination.
   *
   * @param params a list of search parameters to apply
   * @return the total number of articles matching all search criteria
   */
  override def searchCount(params: List[ArticleRepository.SearchParam]): Int < Effect =
    Transactional:
      sql"""SELECT count(*)
           FROM articles a
           JOIN profiles p ON a.author_id = p.user_id
           WHERE (${params.author.isEmpty} OR p.name = ${params.author.getOrElse("")})
           AND (${params.tag.isEmpty} OR EXISTS (
             SELECT 1 FROM tags t2
             WHERE t2.article_id = a.id
             AND t2.name = ${params.tag.getOrElse("")}
           ))
           AND (${params.favoriteBy.isEmpty} OR EXISTS (
             SELECT 1 FROM favorites fav
             JOIN profiles fp ON fav.user_id = fp.user_id
             WHERE fav.article_id = a.id
             AND fp.name = ${params.favoriteBy.getOrElse("")}
           ))"""
        .query[Int]
        .run()
        .headOption
        .getOrElse(0)

  /**
   * Retrieves a feed of articles for a specific user.
   *
   * The feed consists of articles from authors that the user follows.
   *
   * @param userId the ID of the user for whom to retrieve the feed
   * @param offset the starting index for pagination
   * @param limit  the maximum number of articles to return
   * @return a list of articles in the user's feed
   */
  override def feedOf(userId: Id, offset: Int, limit: Int): List[Article] < Effect =
    Transactional:
      sql"""SELECT
           a.id,
           a.slug, a.title, a.description, a.body, a.author_id,
           (SELECT count(*) FROM favorites f WHERE f.article_id = a.id) as favorite_count,
           ARRAY(SELECT name FROM tags t WHERE t.article_id = a.id) as tags,
           a.created_at, a.updated_at
           FROM articles a
           JOIN profiles p ON a.author_id = p.user_id
           JOIN followers fol ON p.id = fol.followee_id
           WHERE fol.follower_id = $userId
           ORDER BY a.created_at DESC
           OFFSET $offset LIMIT $limit"""
        .query[Article]
        .run()
        .toList

  /**
   * Counts the total number of articles in a user's feed.
   *
   * @param userId the ID of the user whose feed articles to count
   * @return the total count of articles in the user's feed
   */
  override def countFeedOf(userId: Id): Int < Effect =
    Transactional:
      sql"""SELECT count(*)
            FROM articles a
            JOIN profiles p ON a.author_id = p.user_id
            JOIN followers fol ON p.id = fol.followee_id
            WHERE fol.follower_id = $userId"""
        .query[Int]
        .run()
        .headOption
        .getOrElse(0)

  /**
   * Extension methods for extracting search parameters from a list of SearchParam instances.
   *
   * These methods provide convenient access to specific search criteria such as tag, author, and favoriter.
   * Each method uses pattern matching to find the first occurrence of the corresponding SearchParam type
   * and returns it as an Option.
   */
  extension (params: List[ArticleRepository.SearchParam]) {

    /**
     * Extracts the tag filter from the search parameters, if present.
     *
     * @return an Option containing the tag to filter by, or None if not specified
     */
    def tag: Option[String] =
      params.collectFirst { case ArticleRepository.SearchParam.Tag(tag) => tag }

    /**
     * Extracts the author filter from the search parameters, if present.
     *
     * @return an Option containing the author's name to filter by, or None if not specified
     */
    def author: Option[String] =
      params.collectFirst { case ArticleRepository.SearchParam.Author(name) => name }

    /**
     * Extracts the favoriter filter from the search parameters, if present.
     *
     * @return an Option containing the username of the favoriter to filter by, or None if not specified
     */
    def favoriteBy: Option[String] =
      params.collectFirst { case ArticleRepository.SearchParam.FavoriteBy(name) => name }
  }
}
