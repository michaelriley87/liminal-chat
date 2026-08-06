import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ref } from 'vue'

import ChatRoomView from './ChatRoomView.vue'

const mocks = vi.hoisted(() => ({
  route: {
    params: { code: 'ABCDE' },
    query: { name: 'Alice' },
  },
  router: {
    push: vi.fn(),
    replace: vi.fn(),
  },
  connect: vi.fn(),
  disconnect: vi.fn(),
  sendMessage: vi.fn(),
}))

vi.mock('vue-router', () => ({
  useRoute: () => mocks.route,
  useRouter: () => mocks.router,
}))

vi.mock('../composables/useChatRoom', () => ({
  useChatRoom: () => ({
    connectionStatus: ref('connected'),
    errorMessage: ref(''),
    messages: ref([]),
    participants: ref([]),
    connect: mocks.connect,
    disconnect: mocks.disconnect,
    sendMessage: mocks.sendMessage,
  }),
}))

describe('ChatRoomView', () => {
  beforeEach(() => {
    mocks.route.params = { code: 'ABCDE' }
    mocks.route.query = { name: 'Alice' }
    mocks.router.push.mockReset()
    mocks.router.replace.mockReset()
    mocks.connect.mockReset()
    mocks.disconnect.mockReset()
    mocks.sendMessage.mockReset()
  })

  it('connects when route details are valid', () => {
    const wrapper = mount(ChatRoomView, {
      global: { stubs: { RouterLink: true } },
    })

    expect(mocks.connect).toHaveBeenCalledOnce()
    wrapper.unmount()
  })

  it.each([
    [{ code: 'ABCD' }, { name: 'Alice' }],
    [{ code: 'ABCDE' }, { name: 'a'.repeat(25) }],
    [{ code: 'ABCDE' }, {}],
  ])('redirects invalid direct navigation', (params, query) => {
    mocks.route.params = params
    mocks.route.query = query

    const wrapper = mount(ChatRoomView, {
      global: { stubs: { RouterLink: true } },
    })

    expect(mocks.connect).not.toHaveBeenCalled()
    expect(mocks.router.replace).toHaveBeenCalledWith({
      name: 'home',
      query: { error: 'A room code and display name are required.' },
    })
    wrapper.unmount()
  })

  it('clears the composer only when a message is accepted', async () => {
    mocks.sendMessage.mockReturnValueOnce(false).mockReturnValueOnce(true)
    const wrapper = mount(ChatRoomView, {
      global: { stubs: { RouterLink: true } },
    })
    const input = wrapper.get('#message')

    await input.setValue('hello')
    await wrapper.find('.composer').trigger('submit')
    expect(input.element.value).toBe('hello')

    await wrapper.find('.composer').trigger('submit')
    expect(input.element.value).toBe('')
    wrapper.unmount()
  })
})
