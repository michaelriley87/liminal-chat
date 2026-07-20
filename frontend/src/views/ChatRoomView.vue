<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

const roomCode = String(route.params.code).toUpperCase()
const displayName = String(route.query.name || '').trim()

const messages = ref([])
const newMessage = ref('')
const connectionStatus = ref('Connecting...')
const errorMessage = ref('')

let socket = null

onMounted(() => {
  if (!displayName) {
    router.push('/')
    return
  }

  const url =
    `ws://localhost:8080/ws` +
    `?room=${encodeURIComponent(roomCode)}` +
    `&name=${encodeURIComponent(displayName)}`

  socket = new WebSocket(url)

  socket.onopen = () => {
    connectionStatus.value = 'Connected'
  }

  socket.onmessage = (event) => {
    try {
      const message = JSON.parse(event.data)

      messages.value.push(message)
    } catch (error) {
      console.error('Invalid message received:', error)
    }
  }

  socket.onclose = (event) => {
    connectionStatus.value = 'Disconnected'

    if (event.code === 1007) {
      errorMessage.value = 'This room does not exist or has expired.'
    } else if (event.reason) {
      errorMessage.value = event.reason
    }
  }

  socket.onerror = () => {
    errorMessage.value = 'Could not connect to the room.'
  }
})

onBeforeUnmount(() => {
  if (socket && socket.readyState === WebSocket.OPEN) {
    socket.close()
  }
})

function sendMessage() {
  const content = newMessage.value.trim()

  if (!content) {
    return
  }

  if (!socket || socket.readyState !== WebSocket.OPEN) {
    errorMessage.value = 'You are not connected.'
    return
  }

  socket.send(
    JSON.stringify({
      type: 'CHAT_MESSAGE',
      content,
    }),
  )

  newMessage.value = ''
}

function leaveRoom() {
  router.push('/')
}
</script>

<template>
  <main>
    <header>
      <div>
        <h1>Room {{ roomCode }}</h1>
        <p>{{ connectionStatus }} as {{ displayName }}</p>
      </div>

      <button type="button" @click="leaveRoom">Leave</button>
    </header>

    <p v-if="errorMessage">{{ errorMessage }}</p>

    <section>
      <p v-if="messages.length === 0">No messages yet.</p>

      <article v-for="(message, index) in messages" :key="index">
        <strong>{{ message.sender }}</strong>
        <p>{{ message.content }}</p>
      </article>
    </section>

    <form @submit.prevent="sendMessage">
      <input
        v-model="newMessage"
        type="text"
        placeholder="Write a message"
        autocomplete="off"
        :disabled="connectionStatus !== 'Connected'"
      />

      <button type="submit" :disabled="connectionStatus !== 'Connected'">Send</button>
    </form>
  </main>
</template>

<style scoped>
main {
  width: min(100% - 2rem, 48rem);
  margin: 2rem auto;
}

header {
  display: flex;
  justify-content: space-between;
  align-items: start;
  gap: 1rem;
}

section {
  min-height: 20rem;
  margin: 2rem 0;
  padding: 1rem;
  border: 1px solid #ccc;
}

article {
  margin-bottom: 1rem;
}

article p {
  margin: 0.25rem 0 0;
}

form {
  display: flex;
  gap: 0.75rem;
}

input {
  flex: 1;
}

input,
button {
  padding: 0.75rem;
  font: inherit;
}
</style>
