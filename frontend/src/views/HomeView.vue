<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

const displayName = ref('')
const roomCode = ref('')
const errorMessage = ref('')

async function createRoom() {
  errorMessage.value = ''

  try {
    const response = await fetch('http://localhost:8080/rooms', {
      method: 'POST',
    })

    if (!response.ok) {
      throw new Error(`Server returned ${response.status}`)
    }

    const room = await response.json()

    router.push({
      name: 'chat-room',
      params: {
        code: room.code,
      },
      query: {
        name: displayName.value,
      },
    })
  } catch (error) {
    errorMessage.value = 'Could not create room.'
    console.error(error)
  }
}

function joinRoom() {
  errorMessage.value = ''

  router.push({
    name: 'chat-room',
    params: {
      code: roomCode.value.toUpperCase(),
    },
    query: {
      name: displayName.value,
    },
  })
}
</script>

<template>
  <main>
    <h1>Liminal Chat</h1>
    <p>Create a temporary chat room or join an existing one.</p>

    <p v-if="errorMessage">{{ errorMessage }}</p>

    <form @submit.prevent="createRoom">
      <h2>Create a room</h2>

      <label for="create-name">Display name</label>
      <input
        id="create-name"
        v-model.trim="displayName"
        type="text"
        placeholder="Enter your name"
        required
      />

      <button type="submit">Create room</button>
    </form>

    <hr />

    <form @submit.prevent="joinRoom">
      <h2>Join a room</h2>

      <label for="room-code">Room code</label>
      <input
        id="room-code"
        v-model.trim="roomCode"
        type="text"
        placeholder="ABCDE"
        maxlength="5"
        required
      />

      <label for="join-name">Display name</label>
      <input
        id="join-name"
        v-model.trim="displayName"
        type="text"
        placeholder="Enter your name"
        required
      />

      <button type="submit">Join room</button>
    </form>
  </main>
</template>

<style scoped>
main {
  width: min(100% - 2rem, 32rem);
  margin: 4rem auto;
}

form {
  display: grid;
  gap: 0.75rem;
  margin: 2rem 0;
}

input,
button {
  padding: 0.75rem;
  font: inherit;
}

button {
  cursor: pointer;
}

hr {
  margin: 2rem 0;
}
</style>
