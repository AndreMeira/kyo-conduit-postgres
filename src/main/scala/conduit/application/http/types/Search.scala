package conduit.application.http.types

type Search      = (Option[String], Option[String], Option[String], Option[Int], Option[Int])
type NamedSearch = (tag: Option[String], author: Option[String], favorited: Option[String], offset: Option[Int], limit: Option[Int])
