package conduit.application.http.types

import conduit.domain.types.{ ProfileName, TagName }

// Type aliases for commonly used types in the HTTP layer
// This are just for better readability

type Offset      = Int
type Limit       = Int
type Author      = ProfileName
type FavoritedBy = ProfileName
type Page        = (Option[Offset], Option[Limit])
type Search      = (Option[TagName], Option[Author], Option[FavoritedBy], Option[Offset], Option[Limit])
