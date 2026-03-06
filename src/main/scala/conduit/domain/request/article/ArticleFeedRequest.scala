package conduit.domain.request.article

import conduit.domain.model.User

/**
 * Represents a request to retrieve a feed of articles for an authenticated user.
 *
 * This request includes pagination parameters to control the number of articles
 * returned and the starting point for the feed. The requester must be an authenticated
 * user to access their personalized article feed.
 *
 * @param requester the authenticated user making the request
 * @param offset the number of articles to skip before starting to collect the result set
 * @param limit the maximum number of articles to return in the feed
 */
case class ArticleFeedRequest(
  requester: User.Authenticated,
  offset: Int,
  limit: Int,
)
