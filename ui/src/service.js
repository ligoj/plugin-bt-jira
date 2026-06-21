/*
 * Service layer for plugin "bt-jira".
 *
 * Tool-level plugin (lives at `service:bt:jira`). The parent `plugin-bt`
 * delegates the subscription-row hooks to us via its `subPluginIdFor`
 * delegation. Mirrors the legacy `jira.js`:
 *
 *   - renderFeatures   → a home link to the JIRA instance.
 *   - renderDetailsKey → the project key (PKEY) chip.
 *
 * Kept free of Vue SFC imports so it can be unit-tested without a DOM.
 */
import { renderServiceLink, renderDetailsChip, useI18nStore } from '@ligoj/host'

const PARAM_URL = 'service:bt:jira:url'
const PARAM_PKEY = 'service:bt:jira:pkey'

/**
 * Link to the JIRA project browse page. Mirrors the legacy
 * `renderServiceLink('home', url + '/browse/' + pkey, 'service:bt:jira:url-pkey')`.
 * Requires both the base URL and the project key.
 */
function renderFeatures(subscription) {
  const params = subscription?.parameters
  const url = params?.[PARAM_URL]
  const pkey = params?.[PARAM_PKEY]
  if (!url || !pkey) return []
  const { t } = useI18nStore()
  return [renderServiceLink({ icon: 'mdi-home', href: `${url.replace(/\/$/, '')}/browse/${pkey}`, title: t('service:bt:jira:url-pkey') })]
}

/**
 * Project-key chip for the subscription details column. Mirrors the
 * legacy `renderKey('service:bt:jira:pkey')`.
 */
function renderDetailsKey(subscription) {
  const pkey = subscription?.parameters?.[PARAM_PKEY]
  if (!pkey) return null
  const { t } = useI18nStore()
  return renderDetailsChip({ icon: 'mdi-jira', text: pkey, title: t('service:bt:jira:pkey') })
}

export default { renderFeatures, renderDetailsKey }
