/*
 * Contract tests for plugin-bt-jira (tool-level JIRA plugin).
 */
import { describe, it, expect, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useI18nStore } from '@ligoj/host'
import pluginBtJiraDef from '../index.js'

beforeEach(() => {
  setActivePinia(createPinia())
})

describe('plugin-bt-jira contract', () => {
  it('exposes a valid tool-level manifest', () => {
    expect(pluginBtJiraDef.id).toBe('bt-jira')
    expect(typeof pluginBtJiraDef.label).toBe('string')
    expect(pluginBtJiraDef.requires).toEqual(['bt'])
    expect(pluginBtJiraDef.routes).toBeUndefined()
    expect(typeof pluginBtJiraDef.install).toBe('function')
    expect(typeof pluginBtJiraDef.feature).toBe('function')
    expect(pluginBtJiraDef.service).toBeTypeOf('object')
    expect(pluginBtJiraDef.meta).toMatchObject({ icon: expect.any(String), color: expect.any(String) })
  })

  it('merges JIRA parameter i18n on install', () => {
    const i18n = useI18nStore()
    pluginBtJiraDef.install()
    expect(i18n.t('service:bt:jira:jdbc-url')).toBe('JDBC URL')
    expect(i18n.t('service:bt:jira:pkey')).toBe('Project key (PKEY)')
  })

  it('throws for an unknown feature', () => {
    expect(() => pluginBtJiraDef.feature('nope')).toThrow(/no feature "nope"/)
  })

  it('renderFeatures returns the JIRA project browse link when url + pkey are set', () => {
    pluginBtJiraDef.install()
    const vnodes = pluginBtJiraDef.feature('renderFeatures', {
      node: { id: 'service:bt:jira:1' },
      parameters: { 'service:bt:jira:url': 'https://jira.example.org', 'service:bt:jira:pkey': 'LIGOJ' },
    })
    expect(vnodes).toHaveLength(1)
    expect(vnodes[0].__v_isVNode).toBe(true)
    expect(vnodes[0].props.href).toBe('https://jira.example.org/browse/LIGOJ')
    expect(vnodes[0].props.target).toBe('_blank')
  })

  it('renderFeatures returns an empty list without url or pkey', () => {
    pluginBtJiraDef.install()
    expect(pluginBtJiraDef.feature('renderFeatures', { parameters: { 'service:bt:jira:url': 'https://jira.example.org' } })).toEqual([])
    expect(pluginBtJiraDef.feature('renderFeatures', { parameters: { 'service:bt:jira:pkey': 'LIGOJ' } })).toEqual([])
    expect(pluginBtJiraDef.feature('renderFeatures', {})).toEqual([])
  })

  it('renderDetailsKey returns the PKEY chip when present', () => {
    pluginBtJiraDef.install()
    const vnode = pluginBtJiraDef.feature('renderDetailsKey', {
      parameters: { 'service:bt:jira:pkey': 'LIGOJ' },
    })
    expect(vnode).toBeTruthy()
    expect(vnode.__v_isVNode).toBe(true)
  })

  it('renderDetailsKey returns null without a pkey', () => {
    pluginBtJiraDef.install()
    expect(pluginBtJiraDef.feature('renderDetailsKey', { parameters: {} })).toBeNull()
  })
})
