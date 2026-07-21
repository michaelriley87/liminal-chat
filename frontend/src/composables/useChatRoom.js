import { ref } from 'vue'

import { WS_BASE_URL } from '../config'

const ConnectionStatus = Object.freeze({
  CONNECTING: 'connecting',
  CONNECTED: 'connected',
  ERROR: 'connection error',
  DISCONNECTED: 'disconnected',
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

  let socket = null
  let intentionalClose = false

  function appendMessage(message) {
    messages.value.push({
      type: message.type ?? 'CHAT_MESSAGE',
      sender: message.sender ?? '',
      content: message.content ?? '',
      time: timeFormatter.format(new Date()),
    })
  }

  function connect() {
    intentionalClose = false
    connectionStatus.value = ConnectionStatus.CONNECTING
    errorMessage.value = ''

    const query = new URLSearchParams({
      room: roomCode,
      name: displayName,
    })

    socket = new WebSocket(`${WS_BASE_URL}/ws?${query.toString()}`)

    socket.onopen = () => {
      connectionStatus.value = ConnectionStatus.CONNECTED
    }

    socket.onmessage = (event) => {
      try {
        appendMessage(JSON.parse(event.data))
      } catch (error) {
        console.error('Could not read incoming message.', error)
      }
    }

    socket.onerror = () => {
      connectionStatus.value = ConnectionStatus.ERROR
      errorMessage.value = 'The room connection encountered an error.'
    }

    socket.onclose = (event) => {
      connectionStatus.value = ConnectionStatus.DISCONNECTED

      if (intentionalClose) {
        return
      }

      if (event.code === 1007) {
        errorMessage.value = 'This room does not exist or has expired.'
      } else if (event.reason) {
        errorMessage.value = event.reason
      } else {
        errorMessage.value = 'The room connection was closed.'
      }
    }
  }

  function sendMessage(content) {
    const trimmedContent = content.trim()

    if (!trimmedContent || socket?.readyState !== WebSocket.OPEN) {
      return false
    }

    socket.send(
      JSON.stringify({
        type: 'CHAT_MESSAGE',
        content: trimmedContent,
      }),
    )

    return true
  }

  function disconnect(reason = 'Left room') {
    intentionalClose = true

    if (socket?.readyState === WebSocket.OPEN) {
      socket.close(1000, reason)
    }
  }

  return {
    connectionStatus,
    errorMessage,
    messages,
    connect,
    disconnect,
    sendMessage,
  }
}
