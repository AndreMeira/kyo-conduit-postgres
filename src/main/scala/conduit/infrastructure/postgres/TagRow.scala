package conduit.infrastructure.postgres

import conduit.domain.model.Article
import conduit.domain.types.TagName

/**
 * A row of the `tags` table: one tag attached to one article.
 *
 * @param name the tag
 * @param articleId the article the tag is attached to
 */
case class TagRow(name: TagName, articleId: Article.Id)
