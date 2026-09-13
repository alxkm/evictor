# Contributing

Thanks for taking the time. This repository is meant to be read as much as used, so clarity of the
implementation counts as much as correctness.

## Building

```bash
./gradlew build
```

JDK 17 or newer. The build compiles, runs the tests and checks line coverage, which must stay at
80 percent or above.

## Adding an eviction policy

1. Implement `org.cache.CacheService` in a new class under `org.cache`.
2. Reject a non-positive capacity through `Preconditions.positiveCapacity`.
3. Reuse `org.cache.internal.EntryList` when the policy needs an ordered list of entries, instead
   of writing another doubly linked list.
4. Register the class in `CacheContractTest#allCaches`. That suite runs the shared contract against
   every implementation and catches most of the usual mistakes: exceeding the capacity, leaking
   entries after a manual eviction, breaking after `clear`.
5. Add a test class for the behaviour that is specific to the policy, in particular the eviction
   order it promises.
6. Add a factory method to `Caches` and a row to the table in the README.

## Style

- Javadoc on every public type and method, explaining what the policy does and when it is the right
  choice, not just what the parameters are.
- Comments explain why a step is there, not what the next line does.
- English only, in code, comments, commits and pull requests.
- Keep the existing formatting: four spaces, 120 column limit.

## Commits and pull requests

- One logical change per commit, with a short imperative subject line: `fix: ...`, `feat: ...`,
  `test: ...`, `docs: ...`, `build: ...`, `ci: ...`.
- Describe what changed and why in the pull request, and link the issue if there is one.
- Make sure `./gradlew build` passes before opening it.
