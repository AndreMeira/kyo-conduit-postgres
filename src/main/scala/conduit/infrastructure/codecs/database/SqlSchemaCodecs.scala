package conduit.infrastructure.codecs.database

import conduit.domain.types.*
import kyo.{ Chunk, SqlNaming, SqlSchema, SqlType }
import kyo.SqlSchemaInterop.biMap

/**
 * kyo-sql column codecs for the domain value objects, each stored as its underlying type,
 * the SQL type `FavoriteCount` casts to, and the naming convention mapping camelCase
 * fields onto snake_case columns.
 */
object SqlSchemaCodecs:

  given SqlNaming = SqlNaming.SnakeCase

  // UUID-based identifiers
  given SqlSchema.Column[ArticleId]     = biMap(SqlSchema.javaUuid)(ArticleId.apply, id => id)
  given SqlSchema.Column[UserId]        = biMap(SqlSchema.javaUuid)(UserId.apply, id => id)
  given SqlSchema.Column[UserProfileId] = biMap(SqlSchema.javaUuid)(UserProfileId.apply, id => id)

  // Long-based identifiers
  given SqlSchema.Column[CommentId] = biMap(SqlSchema.long)(CommentId.apply, id => id)

  // Timestamps
  given SqlSchema.Column[CreatedAt] = biMap(SqlSchema.javaInstant)(CreatedAt.apply, at => at)
  given SqlSchema.Column[UpdatedAt] = biMap(SqlSchema.javaInstant)(UpdatedAt.apply, at => at)

  // Article fields
  given SqlSchema.Column[ArticleSlug]        = biMap(SqlSchema.string)(ArticleSlug.apply, text => text)
  given SqlSchema.Column[ArticleTitle]       = biMap(SqlSchema.string)(ArticleTitle.apply, text => text)
  given SqlSchema.Column[ArticleDescription] = biMap(SqlSchema.string)(ArticleDescription.apply, text => text)
  given SqlSchema.Column[ArticleBody]        = biMap(SqlSchema.string)(ArticleBody.apply, text => text)
  given SqlSchema.Column[TagName]            = biMap(SqlSchema.string)(TagName.apply, text => text)

  // Article aggregates, read from the favorites and tags of an article
  given SqlSchema.Column[FavoriteCount] = biMap(SqlSchema.int)(FavoriteCount.apply, count => count)
  given SqlType[FavoriteCount]          = SqlType.of(SqlType.Type.Int)
  given SqlSchema.Column[List[TagName]] = biMap(SqlSchema.chunkString)(_.toList.map(TagName(_)), tags => Chunk.from(tags))

  // Comment fields
  given SqlSchema.Column[CommentBody] = biMap(SqlSchema.string)(CommentBody.apply, text => text)

  // Profile fields
  given SqlSchema.Column[ProfileName]      = biMap(SqlSchema.string)(ProfileName.apply, text => text)
  given SqlSchema.Column[ProfileBiography] = biMap(SqlSchema.string)(ProfileBiography.apply, text => text)
  given SqlSchema.Column[ProfileImage]     = biMap(SqlSchema.uri)(ProfileImage.apply, uri => uri)

  // Credentials fields
  given SqlSchema.Column[Email]    = biMap(SqlSchema.string)(Email.apply, text => text)
  given SqlSchema.Column[Password] = biMap(SqlSchema.string)(Password.apply, text => text)
