package playground

import java.util.UUID

object Types:
  opaque type UserId <: UUID        = UUID
  opaque type ArticleId <: UUID     = UUID
  opaque type CommentId <: UUID     = UUID
  opaque type UserName <: String    = String
  opaque type ArticleSlug <: String = String

  def UserId(value: UUID): UserId             = value
  def ArticleId(value: UUID): ArticleId       = value
  def CommentId(value: UUID): CommentId       = value
  def UserName(value: String): UserName       = value
  def ArticleSlug(value: String): ArticleSlug = value
