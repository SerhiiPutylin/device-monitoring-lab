---
name: prepare-commits
description: Prepare purpose-focused commits in task worktrees, using task identifiers when available. Use after completing project changes or when the user requests commits. Stage task changes without separate approval and commit only after approval of the exact messages.
---

# Prepare Commits

Follow the repository's `AGENTS.md`. This skill does not authorize remote Git
writes, history rewriting, or execution outside the repository's permissions.

## Check the task worktree

- Follow the Task Worktrees rules in `AGENTS.md`. For each new task after bootstrap,
  use its own `.worktrees/<task-slug>` worktree and fresh `codex/<task-slug>` branch.
  Creating them is pre-authorized within the repository; use Git commands and do
  not change other worktrees, branches, or user changes.
- Inspect the primary checkout, worktree list, branch, and HEAD. Choose the
  user-specified local base, otherwise local `main` or `master`, and report its
  commit. Do not fetch or access a remote automatically.
- Never commit on `main`, `master`, detached HEAD, or another task's branch.
  The only bootstrap exception is a final commit introducing this workflow,
  explicitly approved by the user as a one-time protected-branch exception.
  Approval of a message alone does not grant that exception.
- If task changes already exist in another worktree, preserve them and agree on
  a safe transfer rather than automatically stashing, resetting, or discarding them.

## Group the changes

- Inspect Git status, the current branch and HEAD, staged changes, and relevant
  unstaged and untracked changes before proposing commits.
- Prepare a commit proposal after completing a meaningful change. Use one commit
  per coherent purpose; split independent changes when each can be reviewed and
  understood separately. Keep tightly coupled changes together.
- Preserve existing user changes. Do not include unrelated changes or pre-existing
  staged content just because they are present. Clarify ownership or scope when
  uncertain. Staging relevant task paths or hunks needs no separate user confirmation;
  preserve unrelated staged content and inspect the staged diff.
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

## Obtain message approval

- Present the ordered commit plan: exact message for each commit, included files
  or hunks and purpose for context, plus validation results or limitations.
- Ask only for approval of the exact message or an explicitly listed batch of
  messages. Do not ask for separate staging or file-list approval.
- Stage task-related paths or hunks without separate confirmation. Do not create
  commits before message approval; approval to implement changes is not message
  approval. If a message changes, request approval of the revised message.
- Message approval applies to the described task and does not authorize unrelated
  content, material task-scope expansion, history rewriting, or a protected-branch
  exception. Resolve unexpected content or scope changes before committing.

## Create and verify approved commits

- Recheck worktree, branch, HEAD, working changes, and the index before staging
  and immediately before each commit. Account for earlier commits in the approved
  batch; stop on unexpected changes or a protected or unrelated branch.
- Stage only task-related paths or hunks. Avoid blanket staging such as `git add .`
  or `git add -A`. Inspect the staged diff before each commit and ensure it matches
  the described task content. Do not overwrite or unstage unrelated user changes.
  Run only checks permitted by the repository's rules; hooks and signing commands
  remain subject to those same rules.
- Create only the approved local commits. Amend, reset, rebase, and other history
  changes require separate, action-specific approval. Never perform remote Git writes.
- Verify resulting messages, file lists, authorship, and remaining Git status.
  Report the worktree path, task branch, each commit's short hash and subject,
  checks performed, and remaining changes so the user can cherry-pick.
- Leave cherry-pick and target-branch integration to the user. Preserve the task
  worktree, branch, and commits after handoff; ask before removing or deleting them.
- If a command fails, inspect the resulting state before retrying. Stop when the
  cause requires additional permission or user input; do not widen the operation.
