package conduit.application.http.types

type Page      = (Option[Int], Option[Int])
type NamedPage = (offset: Option[Int], limit: Option[Int])
