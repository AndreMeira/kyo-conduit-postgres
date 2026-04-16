package conduit.application.http.types

// Type aliases for commonly used types in the HTTP layer
// This are just for better readability

type Offset      = Int
type Limit       = Int
type Tag         = String
type Author      = String
type FavoritedBy = String
type Page        = (Option[Offset], Option[Limit])
type Search      = (Option[Tag], Option[Author], Option[FavoritedBy], Option[Int], Option[Int])
