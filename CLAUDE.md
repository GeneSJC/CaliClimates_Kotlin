# Project Conventions — Micro Climate Finder (Android)

## Branch Naming Convention

Pattern: `<type>/<descriptiveName>`

- **Type prefixes**: `bugfix` (fixing broken/incorrect behavior), `feature` (net-new
  functionality), `refinement` (polishing existing behavior — not a bug, not new
  capability), `layout` (visual/structural UI changes only), `docs`
  (documentation-only changes).
- **Descriptive name**: camelCase, no spaces/underscores (e.g. `barChartLabelFix`).
  Specific enough to be recognizable on its own in a branch list — not a generic
  label like "fix" or "update". No version suffix and no ticket/session ID —
  branches are ephemeral working state, not archived milestones; the git log is
  the permanent record, not the branch name.
- Examples: `bugfix/backButtonDoubleTapBlankScreen`, `feature/repoBookmarking`,
  `refinement/topReposFilterChips`, `layout/issueCardSpacing`.

## Branch Lifecycle — one branch per PR, always

**A branch's job ends the moment its PR merges.** Do not push further commits to
a branch after its PR has merged, even if the next change is small, related, or
would arguably still fit under the same branch name. Once merged:

1. The branch is done. Treat it as closed, not as a place to continue working.
2. The **next** round of changes — however small — gets a **brand-new branch**,
   cut fresh from `develop`, with its own name describing that specific round
   of work.
3. `git fetch origin develop && git checkout -b <type>/<newDescriptiveName> origin/develop`

Why this matters: reusing a branch after its PR merges means a single branch
name ends up representing two (or more) unrelated PRs' worth of intent. That is
exactly what happened on 2026-09-28 with `refinement/caRegionsMapPolish` — a
follow-up fix (the ocean-wash rendering bug) was pushed to the same branch
*after* its original PR (#2) had already merged into `develop`. The result was
a second PR (#3) opened from a branch name that no longer clearly described
what was in it, which cost real time untangling "what am I actually looking at
here?" This rule exists specifically to prevent that confusion from recurring.

Cut new work branches from `develop`, not `main`. Open pull requests back into
`develop`. Delete the branch once its PR merges (or ask a maintainer to, since
Claude's push credentials for this repo cannot delete a remote branch — this
was confirmed via repeated 403 responses on branches unambiguously safe to
delete, not just the session's own protected branch). `develop` merges into
`main` only at stable, portfolio-ready milestones, optionally tagged (e.g.
`v0.5.0`).

**Note on Claude Code sessions**: Claude Code on the web auto-assigns each
session its own working branch (e.g. `claude/tender-planck-fzwpr5`) — that name
is not configurable and does not follow this convention. It is session-scoped
scratch space only, not a place deliverable work stays. A single session can
carry several distinct iterations of work; each one gets its own properly-named
branch cut from `develop` per the pattern above, merged into `develop` in turn.

## PR-Based Merge Workflow

Work branches merge into `develop` via a GitHub pull request, not a local git
merge + direct push.

1. Push the work branch once committed.
2. Open a pull request targeting `develop` (not `main`).
3. The repo owner reviews the diff on GitHub.
4. The repo owner clicks "Merge pull request" on GitHub — this is the step that
   actually finalizes it. Claude merges a PR itself only when explicitly told
   to, for something already approved.
5. With "Automatically delete head branches" enabled in repo settings, GitHub
   deletes the branch on its own once merged — no manual cleanup step needed.

If that repo setting is off, merged branches need manual deletion from GitHub's
branch list, since Claude's push credentials cannot delete a remote branch.
