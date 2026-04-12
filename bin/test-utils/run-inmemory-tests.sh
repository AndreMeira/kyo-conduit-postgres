#!/usr/bin/env bash
set -euo pipefail

SPECS=(
  conduit.infrastructure.inmemory.InMemoryArticleRepositorySpec
  conduit.infrastructure.inmemory.InMemoryCommentRepositorySpec
  conduit.infrastructure.inmemory.InMemoryCredentialsRepositorySpec
  conduit.infrastructure.inmemory.InMemoryFavoriteRepositorySpec
  conduit.infrastructure.inmemory.InMemoryFollowerRepositorySpec
  conduit.infrastructure.inmemory.InMemoryTagRepositorySpec
  conduit.infrastructure.inmemory.InMemoryUserProfileRepositorySpec
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
