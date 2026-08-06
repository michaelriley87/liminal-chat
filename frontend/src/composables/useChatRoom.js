import { ref } from 'vue'

import { WS_BASE_URL } from '../config'
import { MAX_MESSAGE_LENGTH } from '../validation'

const ConnectionStatus = Object.freeze({
  CONNECTING: 'connecting',
  CONNECTED: 'connected',
  ERROR: 'connection error',
  DISCONNECTED: 'disconnected',
})

const MessageType = Object.freeze({
  CHAT: 'CHAT_MESSAGE',
  SYSTEM: 'SYSTEM_MESSAGE',
  PARTICIPANTS: 'PARTICIPANT_LIST',
})

const CloseCode = Object.freeze({
  ABNORMAL: 1006,
  ROOM_UNAVAILABLE: 4001,
  NAME_TAKEN: 4002,
  INVALID_CONNECTION: 4003,
})

const timeFormatter = new Intl.DateTimeFormat([], {
  hour: '2-digit',
  minute: '2-digit',
  hour12: false,
})

export function useChatRoom(roomCode, displayName) {
  const connectionStatus = ref(ConnectionStatus.CONNECTING)
  const errorMessage = ref('')
  const messages = ref([])
  const participants = ref([])

  let socket = null
  let intentionalClose = false
  let hasConnected = false

  function appendMessage(message) {
    messages.value.push({
      type: message.type ?? MessageType.CHAT,
      sender: message.sender ?? '',
      content: message.content ?? '',
      time: timeFormatter.format(new Date()),
    })
  }

  function handleIncomingMessage(message) {
    if (!message || typeof message !== 'object') {
      return
    }

    if (message.type === MessageType.PARTICIPANTS) {
      participants.value = Array.isArray(message.participants)
        ? message.participants.filter((participant) => typeof participant === 'string')
        : []
      return
    }

    if (
      (message.type !== MessageType.CHAT && message.type !== MessageType.SYSTEM) ||
      typeof message.content !== 'string' ||
      !message.content ||
      (message.type === MessageType.CHAT && typeof message.sender !== 'string')
    ) {
      return
    }

    appendMessage(message)
  }

  function getCloseMessage(event) {
    if (event.code === CloseCode.ROOM_UNAVAILABLE) {
      return 'This room no longer exists.'
    }

    if (event.code === CloseCode.NAME_TAKEN) {
      return 'That display name is already in use in this room.'
    }

    if (event.code === CloseCode.INVALID_CONNECTION) {
      return 'The room code or display name is invalid.'
    }

    if (event.code === CloseCode.ABNORMAL) {
      return hasConnected
        ? 'The connection to the room was lost.'
        : 'Could not connect to the room.'
    }

    if (event.reason) {
      return event.reason
    }

    return 'The room connection was closed.'
  }

  function connect() {
    intentionalClose = false
    hasConnected = false
    connectionStatus.value = ConnectionStatus.CONNECTING
    errorMessage.value = ''

    const query = new URLSearchParams({
      room: roomCode,
      name: displayName,
    })

    socket = new WebSocket(`${WS_BASE_URL}/ws?${query.toString()}`)

    socket.onopen = () => {
      hasConnected = true
      connectionStatus.value = ConnectionStatus.CONNECTED
    }

    socket.onmessage = (event) => {
      try {
        handleIncomingMessage(JSON.parse(event.data))
      } catch (error) {
        console.error('Could not read incoming message.', error)
      }
    }

    socket.onerror = () => {
      connectionStatus.value = ConnectionStatus.ERROR
    }

    socket.onclose = (event) => {
      connectionStatus.value = ConnectionStatus.DISCONNECTED
      participants.value = []

      if (intentionalClose) {
        return
      }

      errorMessage.value = getCloseMessage(event)
    }
  }

  function sendMessage(content) {
    if (typeof content !== 'string') {
      return false
    }

    const trimmedContent = content.trim()

    if (
      !trimmedContent ||
      trimmedContent.length > MAX_MESSAGE_LENGTH ||
      socket?.readyState !== WebSocket.OPEN
    ) {
      return false
    }

    socket.send(
      JSON.stringify({
        type: MessageType.CHAT,
        content: trimmedContent,
      }),
    )

    return true
  }

  function disconnect(reason = 'Left room') {
    intentionalClose = true

    if (socket?.readyState === WebSocket.OPEN || socket?.readyState === WebSocket.CONNECTING) {
      socket.close(1000, reason)
    }
  }

  return {
    connectionStatus,
    errorMessage,
    messages,
    participants,
    connect,
    disconnect,
    sendMessage,
  }
}
