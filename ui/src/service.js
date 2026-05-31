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
import { h } from 'vue'
import { VBtn, VChip, VIcon, useI18nStore } from '@ligoj/host'

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
  return [
    h(
      VBtn,
      {
        icon: true,
        size: 'small',
        variant: 'text',
        href: `${url.replace(/\/$/, '')}/browse/${pkey}`,
        target: '_blank',
        rel: 'noopener noreferrer',
        title: t('service:bt:jira:url-pkey'),
      },
      () => h(VIcon, { size: 'small' }, () => 'mdi-home'),
    ),
  ]
}

/**
 * Project-key chip for the subscription details column. Mirrors the
 * legacy `renderKey('service:bt:jira:pkey')`.
 */
function renderDetailsKey(subscription) {
  const pkey = subscription?.parameters?.[PARAM_PKEY]
  if (!pkey) return null
  const { t } = useI18nStore()
  return h(
    VChip,
    { size: 'small', variant: 'tonal', class: 'mr-1', title: t('service:bt:jira:pkey') },
    () => [h(VIcon, { start: true, size: 'small' }, () => 'mdi-jira'), ' ', String(pkey)],
  )
}

export default { renderFeatures, renderDetailsKey }
