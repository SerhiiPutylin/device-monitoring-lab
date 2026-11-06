---
name: prepare-commits
description: Prepare purpose-focused local commits, using task identifiers when available, after completing changes in this repository or when the user requests commits. Propose a concrete commit plan and create commits only after explicit approval.
---

# Prepare Commits

Follow the repository's `AGENTS.md`. This skill does not authorize remote Git
writes, history rewriting, or execution outside the repository's permissions.

## Group the changes

- Inspect Git status, the current branch and HEAD, staged changes, and relevant
  unstaged and untracked changes before proposing commits.
- Prepare a commit proposal after completing a meaningful change. Use one commit
  per coherent purpose; split independent changes when each can be reviewed and
  understood separately. Keep tightly coupled changes together.
- Preserve existing user changes. Do not include unrelated changes or pre-existing
  staged content just because they are present. Clarify ownership or scope when
  uncertain, and leave the index untouched until the plan is approved.
- Keep secrets, local environment files, IDE metadata, and generated output out
  of proposed commits unless the user explicitly approves a specific exception.

## Write the messages

Once task tracking is established, the subject has this structure:

```text
TASK-ID purpose-focused summary
```

The user has approved a temporary exception while task tracking is not established:
use only the purpose-focused summary, without a task identifier or placeholder.
Do not repeatedly ask for an identifier during this period.

- When a task or GitHub Issue identifier is provided or already established for
  these changes, put it first using its actual form, such as `#42` or `DML-42`.
- Once task tracking is established, ask if the required identifier is missing or
  ambiguous. Never invent an identifier or create or look up a remote issue without
  permission. The temporary exception above allows omission only while task tracking
  is not established.
- Explain why the change is useful or what behavior it enables, rather than
  listing edited files. Prefer a concise, imperative English summary unless the
  user requests another language. Do not add a Conventional Commits prefix unless
  requested; when present, the task identifier comes first.
- Add an optional body when needed to explain motivation, tradeoffs, or validation.
  Describe checks accurately, including any that were not run.

Hypothetical example, using an identifier that must not be reused without context:

```text
#42 Prevent local machine settings from entering version control
```

## Obtain approval

- Present the complete, ordered plan: exact message for each commit, included
  files or hunks, purpose, and relevant validation results or limitations.
- Request explicit approval for the proposed commit or an explicitly listed batch
  of commits. Approval to implement changes or create this skill is not approval
  to stage files or create commits.
- Do not stage or commit before approval. If the proposed content or messages
  change materially afterward, obtain approval for the revised plan.

## Create and verify approved commits

- Recheck HEAD, branch, working changes, and the index against the approved plan
  immediately before staging. If they differ from the reviewed state, pause and
  reconcile the differences with the user; do not overwrite or unstage their work.
- Stage only approved paths or hunks. Avoid blanket staging such as `git add .`
  or `git add -A`. Inspect the staged diff before each commit and ensure it matches
  the approved content. Run only checks permitted by the repository's rules;
  hooks and signing commands remain subject to those same rules.
- Create only the approved local commits. Amend, reset, rebase, and other history
  changes require separate, action-specific approval. Never perform remote Git writes.
- Verify the resulting messages, file lists, and remaining Git status. Report
  each commit's short hash and subject, checks performed, and remaining changes.
- If a command fails, inspect the resulting state before retrying. Stop when the
  cause requires additional permission or user input; do not widen the operation.
