import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import HomeView from './HomeView.vue'

const mocks = vi.hoisted(() => ({
  route: { query: {} },
  router: {
    push: vi.fn(),
    replace: vi.fn(),
  },
}))

vi.mock('vue-router', () => ({
  useRoute: () => mocks.route,
  useRouter: () => mocks.router,
}))

describe('HomeView', () => {
  beforeEach(() => {
    mocks.route.query = {}
    mocks.router.push.mockReset()
    mocks.router.replace.mockReset()
    vi.unstubAllGlobals()
  })

  it('validates a display name before creating a room', async () => {
    const wrapper = mount(HomeView)

    await wrapper.find('form').trigger('submit')

    expect(wrapper.get('[role="alert"]').text()).toBe('Enter a display name.')
    expect(mocks.router.push).not.toHaveBeenCalled()
  })

  it('creates a room and navigates using normalised server data', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue({
        ok: true,
        json: vi.fn().mockResolvedValue({ code: 'ab12c' }),
      }),
    )
    const wrapper = mount(HomeView)

    await wrapper.get('#display-name').setValue('  Alice  ')
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(fetch).toHaveBeenCalledWith('/rooms', { method: 'POST' })
    expect(mocks.router.push).toHaveBeenCalledWith({
      name: 'chat-room',
      params: { code: 'AB12C' },
      query: { name: 'Alice' },
    })
  })

  it('reports invalid room data returned by the server', async () => {
    vi.spyOn(console, 'error').mockImplementation(() => {})
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue({
        ok: true,
        json: vi.fn().mockResolvedValue({ code: 'invalid' }),
      }),
    )
    const wrapper = mount(HomeView)

    await wrapper.get('#display-name').setValue('Alice')
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(wrapper.get('[role="alert"]').text()).toBe('Could not create room.')
    expect(mocks.router.push).not.toHaveBeenCalled()
  })

  it('validates and normalises join details', async () => {
    const wrapper = mount(HomeView)
    await wrapper.findAll('.mode-switch button')[1].trigger('click')
    await wrapper.get('#display-name').setValue(' Bob ')
    await wrapper.get('#room-code').setValue(' ab12c ')
    await wrapper.find('form').trigger('submit')

    expect(mocks.router.push).toHaveBeenCalledWith({
      name: 'chat-room',
      params: { code: 'AB12C' },
      query: { name: 'Bob' },
    })
  })

  it('rejects an invalid join code', async () => {
    const wrapper = mount(HomeView)
    await wrapper.findAll('.mode-switch button')[1].trigger('click')
    await wrapper.get('#display-name').setValue('Bob')
    await wrapper.get('#room-code').setValue('AB-12')
    await wrapper.find('form').trigger('submit')

    expect(wrapper.get('[role="alert"]').text()).toContain('exactly five')
    expect(mocks.router.push).not.toHaveBeenCalled()
  })
})
