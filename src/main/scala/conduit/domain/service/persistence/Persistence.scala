package conduit.domain.service.persistence

import Database.Transaction

class Persistence[Tx <: Transaction](
  val articles: ArticleRepository[Tx],
  val users: UserProfileRepository[Tx],
  val followers: FollowerRepository[Tx],
  val favorites: FavoriteRepository[Tx],
  val credentials: CredentialsRepository[Tx],
  val comments: CommentRepository[Tx],
  val tags: TagRepository[Tx],
)
