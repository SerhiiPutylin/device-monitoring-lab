---
name: write-tests
description: Write or update Java unit and integration tests for new features, bug fixes, and behavior changes in Device Monitoring Lab.
---

# Write Tests

Follow `AGENTS.md`. Treat tests as part of each new feature and add a regression test for each bug fix. Derive expected results from the agreed behavior and acceptance criteria.

## Test coverage

- Add unit tests for the new or changed logic and integration tests for the boundaries the feature affects. If one level has no distinct behavior to verify, explain why instead of duplicating coverage.
- Cover the normal path and relevant boundaries and failure cases. Assert observable results and side effects; avoid testing private methods or repeating the production algorithm in assertions.
- Keep production changes limited to the authorized task. If testing needs a seam, prefer a small extraction or injectable collaborator that preserves behavior.

## Project stack

- Use JUnit Jupiter for both unit and integration tests and Maven Surefire to run them. Use the Java and dependency versions declared in `pom.xml`.
- Place tests in `src/test/java` in the matching package. Follow the existing `*Test` and `*IntegrationTest` naming conventions.
- Use JUnit assertions and parameterized tests for cases that share a contract. Prefer standard JDK fixtures and simple test doubles. Reuse existing test libraries; propose a new dependency only when it solves a concrete testing need.

## Run and report

- Make integration tests self-contained: use isolated local resources, explicit readiness, bounded waits, and deterministic cleanup. For socket tests use loopback and a dynamically assigned port. Avoid sleeps for synchronization.
- Run the affected tests and relevant regression suite when authorized. Prefer Maven offline mode when dependencies are cached. Writing tests does not itself authorize execution or downloads; follow the current task scope and `AGENTS.md`.
- Report what is covered and what actually ran, including failures or checks that remain unverified.
