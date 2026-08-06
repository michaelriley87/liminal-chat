import { beforeEach, describe, expect, it, vi } from 'vitest'

import { useChatRoom } from './useChatRoom'

class MockWebSocket {
  static CONNECTING = 0
  static OPEN = 1
  static instances = []

  constructor(url) {
    this.url = url
    this.readyState = MockWebSocket.CONNECTING
    this.sentMessages = []
    MockWebSocket.instances.push(this)
  }

  open() {
    this.readyState = MockWebSocket.OPEN
    this.onopen?.()
  }

  receive(payload) {
    this.onmessage?.({ data: JSON.stringify(payload) })
  }

  send(payload) {
    this.sentMessages.push(payload)
  }

  close(code, reason) {
    this.closeArguments = { code, reason }
    this.readyState = 3
  }

  finishClose(code, reason = '') {
    this.readyState = 3
    this.onclose?.({ code, reason })
  }
}

describe('useChatRoom', () => {
  beforeEach(() => {
    MockWebSocket.instances = []
    vi.stubGlobal('WebSocket', MockWebSocket)
  })

  it('connects with encoded room details and reports connection state', () => {
    const chatRoom = useChatRoom('ABCDE', 'Alice Smith')

    chatRoom.connect()
    const socket = MockWebSocket.instances[0]

    expect(socket.url).toBe('ws://localhost:8080/ws?room=ABCDE&name=Alice+Smith')
    expect(chatRoom.connectionStatus.value).toBe('connecting')

    socket.open()
    expect(chatRoom.connectionStatus.value).toBe('connected')
  })

  it('sends trimmed valid messages and rejects invalid messages', () => {
    const chatRoom = useChatRoom('ABCDE', 'Alice')
    chatRoom.connect()
    const socket = MockWebSocket.instances[0]
    socket.open()

    expect(chatRoom.sendMessage('  hello  ')).toBe(true)
    expect(JSON.parse(socket.sentMessages[0])).toEqual({
      type: 'CHAT_MESSAGE',
      content: 'hello',
    })
    expect(chatRoom.sendMessage('   ')).toBe(false)
    expect(chatRoom.sendMessage('a'.repeat(501))).toBe(false)
    expect(chatRoom.sendMessage(null)).toBe(false)
    expect(socket.sentMessages).toHaveLength(1)
  })

  it('accepts known server messages and ignores malformed protocol messages', () => {
    const chatRoom = useChatRoom('ABCDE', 'Alice')
    chatRoom.connect()
    const socket = MockWebSocket.instances[0]

    socket.receive({ type: 'PARTICIPANT_LIST', participants: ['Alice', 42, 'Bob'] })
    socket.receive({ type: 'CHAT_MESSAGE', sender: 'Bob', content: 'Hello' })
    socket.receive({ type: 'SYSTEM_MESSAGE', content: 'Bob joined the room' })
    socket.receive({ type: 'UNKNOWN', content: 'ignored' })
    socket.receive({ type: 'CHAT_MESSAGE', sender: 42, content: 'ignored' })

    expect(chatRoom.participants.value).toEqual(['Alice', 'Bob'])
    expect(chatRoom.messages.value).toHaveLength(2)
    expect(chatRoom.messages.value.map((message) => message.content)).toEqual([
      'Hello',
      'Bob joined the room',
    ])
  })

  it.each([
    [4001, 'This room no longer exists.'],
    [4002, 'That display name is already in use in this room.'],
    [4003, 'The room code or display name is invalid.'],
  ])('maps close code %i to a useful error', (code, expectedMessage) => {
    const chatRoom = useChatRoom('ABCDE', 'Alice')
    chatRoom.connect()

    MockWebSocket.instances[0].finishClose(code)

    expect(chatRoom.connectionStatus.value).toBe('disconnected')
    expect(chatRoom.errorMessage.value).toBe(expectedMessage)
  })

  it('distinguishes initial connection failure from a lost connection', () => {
    const initialFailure = useChatRoom('ABCDE', 'Alice')
    initialFailure.connect()
    MockWebSocket.instances[0].finishClose(1006)
    expect(initialFailure.errorMessage.value).toBe('Could not connect to the room.')

    const lostConnection = useChatRoom('ABCDE', 'Bob')
    lostConnection.connect()
    const socket = MockWebSocket.instances[1]
    socket.open()
    socket.finishClose(1006)
    expect(lostConnection.errorMessage.value).toBe('The connection to the room was lost.')
  })

  it('closes connecting and open sockets intentionally without reporting an error', () => {
    const chatRoom = useChatRoom('ABCDE', 'Alice')
    chatRoom.connect()
    const socket = MockWebSocket.instances[0]

    chatRoom.disconnect('Page closed')
    expect(socket.closeArguments).toEqual({ code: 1000, reason: 'Page closed' })

    socket.finishClose(1000, 'Page closed')
    expect(chatRoom.errorMessage.value).toBe('')
  })
})
