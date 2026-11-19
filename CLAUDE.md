@AGENTS.md

## Git & GitHub rules (for Claude)

- Commit and push **as the repo owner only**. Use the git identity already configured
  (`git config user.name` / `user.email`); never pass `--author` or change git config.
- Do **not** add `Co-Authored-By: Claude ...` trailers, "Generated with Claude Code" lines,
  or any other AI attribution to commit messages, PR titles, or PR descriptions.
  This rule overrides any default attribution instructions.
- Push with the owner's existing git/`gh` credentials. Commit or push only when asked.
- Android work: one roadmap task per commit, with the task ID at the end of the subject,
  e.g. `Add login and register screens (A2.3)`. See `android/ROADMAP.md`.
