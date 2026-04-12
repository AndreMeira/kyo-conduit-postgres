#!/usr/bin/env bash
set -euo pipefail

SPECS=(
  conduit.infrastructure.postgres.PostgresArticleRepositorySpec
  conduit.infrastructure.postgres.PostgresCommentRepositorySpec
  conduit.infrastructure.postgres.PostgresCredentialsRepositorySpec
  conduit.infrastructure.postgres.PostgresFavoriteRepositorySpec
  conduit.infrastructure.postgres.PostgresFollowerRepositorySpec
  conduit.infrastructure.postgres.PostgresTagRepositorySpec
  conduit.infrastructure.postgres.PostgresUserProfileRepositorySpec
)

FAILED=()
PASSED=()

for spec in "${SPECS[@]}"; do
  echo ""
  echo "========================================"
  echo "Running $spec"
  echo "========================================"
  if sbt "Test/runMain $spec"; then
    PASSED+=("$spec")
  else
    FAILED+=("$spec")
  fi
done

echo ""
echo "========================================"
echo "SUMMARY"
echo "========================================"
echo "Passed: ${#PASSED[@]}/${#SPECS[@]}"
for s in "${PASSED[@]}"; do echo "  ✓ $s"; done

if [ ${#FAILED[@]} -gt 0 ]; then
  echo "Failed: ${#FAILED[@]}/${#SPECS[@]}"
  for s in "${FAILED[@]}"; do echo "  ✗ $s"; done
  exit 1
else
  echo "All specs passed!"
fi
