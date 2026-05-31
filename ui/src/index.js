/*
 * Plugin "bt-jira" — JIRA implementation of plugin-bt.
 *
 * Tool-level plugin: lives at `service:bt:jira` in the node tree. It
 * augments the parent `plugin-bt` via:
 *   - i18n: JIRA parameter labels (JDBC connection, project key, …) for
 *     the subscribe wizard's auto-rendered parameter form.
 *   - feature('renderFeatures', subscription): the JIRA home link.
 *   - feature('renderDetailsKey', subscription): the project-key chip.
 *
 * The parent `plugin-bt` merges these into its subscription-row output
 * through its `subPluginIdFor(...)` delegation hook.
 *
 * Authored as source — compiled to `/main/bt-jira/vue/index.js` by Vite.
 */
import { useI18nStore } from '@ligoj/host'
import enMessages from './i18n/en.js'
import frMessages from './i18n/fr.js'
import service from './service.js'

const features = {
  renderFeatures: service.renderFeatures,
  renderDetailsKey: service.renderDetailsKey,
}

export default {
  id: 'bt-jira',
  label: 'JIRA',
  // Declared dependency on the parent service-level plugin: it provides
  // the delegation hook that pulls our VNodes into subscription rows.
  requires: ['bt'],
  install() {
    const i18n = useI18nStore()
    i18n.merge(enMessages, 'en')
    i18n.merge(frMessages, 'fr')
  },
  feature(action, ...args) {
    const fn = features[action]
    if (!fn) throw new Error(`Plugin "bt-jira" has no feature "${action}"`)
    return fn(...args)
  },
  service,
  meta: { icon: 'mdi-jira', color: 'blue-darken-3' },
}

export { service }
