-- Align column names with the domain fields they hold: UserProfile.biography and
-- UserProfile.Follower.profileId.

ALTER TABLE profiles RENAME COLUMN bio TO biography;

ALTER TABLE followers RENAME COLUMN followee_id TO profile_id;
ALTER TABLE followers RENAME CONSTRAINT followers_followee_id_fkey TO followers_profile_id_fkey;
ALTER INDEX idx_followers_followee_id RENAME TO idx_followers_profile_id;
