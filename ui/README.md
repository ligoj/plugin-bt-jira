# plugin-bt-jira — Vue UI

Vue source for the **bt-jira** tool plugin (`service:bt:jira`), the JIRA
implementation of the `bt` (Bug Tracking) service. Compiled by Vite into
the Maven plugin JAR at
`../src/main/resources/META-INF/resources/webjars/bt-jira/vue/`, served by
the host at `/main/bt-jira/vue/index.js`.

Tool-level plugin — see the host's `app-ui/REWRITE_VUEJS.md`. It ships:

- **i18n** — JIRA parameter labels (`service:bt:jira:*`) for the subscribe
  wizard's auto-rendered parameter form.
- **`renderFeatures`** — a home link to the JIRA instance.
- **`renderDetailsKey`** — the project-key (PKEY) chip.

It declares `requires: ['bt']`; the parent `plugin-bt` merges the row
features above via its delegation hook (`subPluginIdFor` maps
`service:bt:jira:*` → `bt-jira`).

## Commands

```bash
npm install
npm run build   # → ../src/main/resources/.../webjars/bt-jira/vue/
npm run lint
npm test        # vitest — manifest + feature contract tests
npm run dev     # standalone dev harness on :5181
```
