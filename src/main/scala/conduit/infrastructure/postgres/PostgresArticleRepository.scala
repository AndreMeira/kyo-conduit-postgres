package conduit.infrastructure.postgres

import conduit.domain.model.{ Article, User, UserProfile }
import conduit.domain.service.persistence.ArticleRepository
import conduit.domain.service.persistence.ArticleRepository.SearchParam
import conduit.domain.types.*
import conduit.infrastructure.codecs.database.SqlSchemaCodecs.given
import PostgresTransaction.Transactional
import kyo.*

/**
 * Postgres implementation of [[ArticleRepository]], over the `articles` table. An article is read
 * with its favorite count and its tags, gathered from the `favorites` and `tags` tables.
 */
class PostgresArticleRepository extends ArticleRepository[PostgresTransaction]:

  /**
   * Finds an article by its id.
   *
   * @param id the article
   * @return the article, or `Absent`; aborts with `PostgresDatabase.Error.QueryError` when the query fails
   */
  override def find(id: Article.Id): Maybe[Article] < Effect =
    Transactional:
      articles(
        Sql.from[Article.Data]("i", "articles").where(_.i.id == id).select(_.i.id)
      ).run.map(rows => first(rows.map(Article.apply.tupled)))

  /**
   * Finds an article by its slug.
   *
   * @param slug the slug
   * @return the article, or `Absent`; aborts with `PostgresDatabase.Error.QueryError` when the query fails
   */
  override def findBySlug(slug: String): Maybe[Article] < Effect =
    Transactional:
      articles(
        Sql.from[Article.Data]("i", "articles").where(_.i.slug == ArticleSlug(slug)).select(_.i.id)
      ).run.map(rows => first(rows.map(Article.apply.tupled)))

  /**
   * Whether an article exists.
   *
   * @param id the article
   * @return true when it exists; aborts with `PostgresDatabase.Error.QueryError` when the query fails
   */
  override def exists(id: Article.Id): Boolean < Effect =
    Transactional:
      Sql.from[Article.Data]("a", "articles").where(_.a.id == id).limit(1).run.map(_.nonEmpty)

  /**
   * Saves a new article.
   *
   * @param article the article's own fields
   * @return unit; aborts with `PostgresDatabase.Error.QueryError` when the insert fails
   */
  override def save(article: Article.Data): Unit < Effect =
    Transactional.single("articles"):
      Sql.insert[Article.Data]("articles").values(article).run.map(_.affectedRows)

  /**
   * Replaces an article's own fields; its favorites and tags are left as they are.
   *
   * @param article the article, carrying its new fields
   * @return unit; aborts with `PostgresDatabase.Error.QueryError` when the update fails, or with
   *         `PostgresDatabase.Error.UnexpectedRowCount` when no article has its id
   */
  override def update(article: Article.Data): Unit < Effect =
    Transactional.single("articles"):
      Sql
        .update[Article.Data]("articles")
        .set(
          _.slug        := article.slug,
          _.title       := article.title,
          _.description := article.description,
          _.body        := article.body,
          _.authorId    := article.authorId,
          _.createdAt   := article.createdAt,
          _.updatedAt   := article.updatedAt,
        )
        .where(_.id == article.id)
        .run

  /**
   * Deletes an article.
   *
   * @param id the article
   * @return unit; aborts with `PostgresDatabase.Error.QueryError` when the delete fails, or with
   *         `PostgresDatabase.Error.UnexpectedRowCount` when no article has this id
   */
  override def delete(id: Article.Id): Unit < Effect =
    Transactional.single("articles"):
      Sql.delete[Article.Data]("articles").where(_.id == id).run

  /**
   * Searches articles matching every given filter, newest first.
   *
   * @param params the filters: by tag, by author username, by username of a user who favorited it
   * @param offset the number of matching articles to skip
   * @param limit the maximum number of articles to return
   * @return the matching articles; aborts with `PostgresDatabase.Error.QueryError` when the query fails
   */
  override def search(params: List[SearchParam], offset: Int, limit: Int): List[Article] < Effect =
    Transactional:
      articles(
        matching(params).select(_.a.id)
      ).limit(limit, offset).run.map(_.toList.map(Article.apply.tupled))

  /**
   * Counts the articles matching every given filter.
   *
   * @param params the filters, as for `search`
   * @return the number of matching articles; aborts with `PostgresDatabase.Error.QueryError` when the query fails
   */
  override def searchCount(params: List[SearchParam]): Int < Effect =
    Transactional:
      matching(params).count.run.map(counted)

  /**
   * The articles written by authors the user follows, newest first.
   *
   * @param userId the user
   * @param offset the number of articles to skip
   * @param limit the maximum number of articles to return
   * @return the articles; aborts with `PostgresDatabase.Error.QueryError` when the query fails
   */
  override def feedOf(userId: User.Id, offset: Int, limit: Int): List[Article] < Effect =
    Transactional:
      articles(
        followedBy(userId).select(_.a.id)
      ).limit(limit, offset).run.map(_.toList.map(Article.apply.tupled))

  /**
   * Counts the articles written by authors the user follows.
   *
   * @param userId the user
   * @return the number of articles; aborts with `PostgresDatabase.Error.QueryError` when the query fails
   */
  override def countFeedOf(userId: User.Id): Int < Effect =
    Transactional:
      followedBy(userId).count.run.map(counted)

  /**
   * The articles whose ids the given query selects, each with its favorite count and tags, newest first.
   *
   * @param ids the query selecting the article ids
   * @return the query reading those articles, as rows holding `Article`'s fields in order
   */
  private def articles(ids: Sql.Query[Article.Id]) =
    Sql
      .from[Article.Data]("a", "articles")
      .where(_.a.id.in(ids))
      .select(row =>
        (
          row.a.id,
          row.a.slug,
          row.a.title,
          row.a.description,
          row.a.body,
          row.a.authorId,
          Sql.from[Article.Favorite]("f", "favorites").where(_.f.articleId == row.a.id).count.cast[FavoriteCount],
          Sql.call[List[TagName]]("ARRAY", Sql.from[TagRow]("t", "tags").where(_.t.articleId == row.a.id).select(_.t.name)),
          row.a.createdAt,
          row.a.updatedAt,
        )
      )
      .orderBy(_.a.createdAt.desc)

  /**
   * The articles, joined to their author's profile, matching every given filter. A filter the
   * parameters do not set matches every article.
   *
   * @param params the filters
   * @return the filtered query, its rows keyed `a` for the article and `p` for the profile
   */
  private def matching(params: List[SearchParam]) =
    val author     = params.collectFirst { case SearchParam.Author(username) => username }
    val tag        = params.collectFirst { case SearchParam.Tag(value) => value }
    val favoriteBy = params.collectFirst { case SearchParam.FavoriteBy(username) => username }
    Sql
      .from[Article.Data]("a", "articles")
      .innerJoin(Sql.from[UserProfile]("p", "profiles"))
      .on(row => row.a.authorId == row.p.userId)
      .where(row =>
        (Sql.literal(author.isEmpty) || row.p.name == ProfileName(author.getOrElse("")))
        && (Sql.literal(tag.isEmpty) || Sql
          .from[TagRow]("t", "tags")
          .where(tagged => tagged.t.articleId == row.a.id && tagged.t.name == TagName(tag.getOrElse("")))
          .exists)
        && (Sql.literal(favoriteBy.isEmpty) || Sql
          .from[Article.Favorite]("f", "favorites")
          .innerJoin(Sql.from[UserProfile]("fp", "profiles"))
          .on(fav => fav.f.userId == fav.fp.userId)
          .where(fav => fav.f.articleId == row.a.id && fav.fp.name == ProfileName(favoriteBy.getOrElse("")))
          .exists)
      )

  /**
   * The articles written by authors the user follows.
   *
   * @param userId the user
   * @return the query, its rows keyed `a` for the article
   */
  private def followedBy(userId: User.Id) =
    Sql
      .from[Article.Data]("a", "articles")
      .innerJoin(Sql.from[UserProfile]("p", "profiles"))
      .on(row => row.a.authorId == row.p.userId)
      .innerJoin(Sql.from[UserProfile.Follower]("fol", "followers"))
      .on(row => row.p.id == row.fol.profileId)
      .where(_.fol.followerId == userId)

  /**
   * The article in the first row, if any.
   *
   * @param rows the articles a query returned
   * @return the first one, or `Absent`
   */
  private def first(rows: Chunk[Article]): Maybe[Article] =
    Maybe.fromOption(rows.headOption)

  /**
   * The count a count query returned.
   *
   * @param rows the single row of a count query
   * @return the count, or 0 when the query returned no row
   */
  private def counted(rows: Chunk[Long]): Int =
    rows.headOption.fold(0)(_.toInt)
