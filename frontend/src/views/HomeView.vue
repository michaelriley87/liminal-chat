<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { API_BASE_URL } from '../config'
import {
  getDisplayNameError,
  isValidRoomCode,
  normaliseDisplayName,
  normaliseRoomCode,
} from '../validation'

const route = useRoute()
const router = useRouter()

const mode = ref('create')
const createName = ref('')
const joinName = ref('')
const roomCode = ref('')
const isCreatingRoom = ref(false)

const errorMessage = ref(typeof route.query.error === 'string' ? route.query.error : '')

const activeName = computed({
  get() {
    return mode.value === 'create' ? createName.value : joinName.value
  },
  set(value) {
    if (mode.value === 'create') {
      createName.value = value
    } else {
      joinName.value = value
    }
  },
})

function getValidatedName() {
  const name = normaliseDisplayName(activeName.value)
  const validationError = getDisplayNameError(name)

  if (validationError) {
    errorMessage.value = validationError
    return null
  }

  return name
}

async function createRoom(name) {
  errorMessage.value = ''
  isCreatingRoom.value = true

  try {
    const response = await fetch(`${API_BASE_URL}/rooms`, {
      method: 'POST',
    })

    if (!response.ok) {
      throw new Error(`Server returned ${response.status}`)
    }

    const room = await response.json()
    const createdRoomCode = normaliseRoomCode(room?.code)

    if (!isValidRoomCode(createdRoomCode)) {
      throw new Error('Server returned an invalid room code')
    }

    router.push({
      name: 'chat-room',
      params: {
        code: createdRoomCode,
      },
      query: {
        name,
      },
    })
  } catch (error) {
    errorMessage.value = 'Could not create room.'
    console.error(error)
  } finally {
    isCreatingRoom.value = false
  }
}

function joinRoom(name) {
  const normalisedRoomCode = normaliseRoomCode(roomCode.value)

  if (!isValidRoomCode(normalisedRoomCode)) {
    errorMessage.value = 'Room codes must contain exactly five letters or numbers.'
    return
  }

  errorMessage.value = ''

  router.push({
    name: 'chat-room',
    params: {
      code: normalisedRoomCode,
    },
    query: {
      name,
    },
  })
}

function submitForm() {
  const name = getValidatedName()

  if (!name) {
    return
  }

  if (mode.value === 'create') {
    createRoom(name)
  } else {
    joinRoom(name)
  }
}

onMounted(() => {
  if (typeof route.query.error !== 'string') {
    return
  }

  const remainingQuery = { ...route.query }

  delete remainingQuery.error

  router.replace({
    query: remainingQuery,
  })
})
</script>

<template>
  <main class="page">
    <header class="header">
      <p class="wordmark">LIMINAL</p>
      <p class="status">temporary private chat</p>
    </header>

    <section class="content">
      <div class="intro">
        <h1>
          Talk for
          <span>now.</span>
        </h1>

        <p>
          Create a temporary room or enter one using a shared code. Rooms disappear after
          inactivity.
        </p>
      </div>

      <div class="panel">
        <nav class="mode-switch" aria-label="Room action">
          <button type="button" :class="{ active: mode === 'create' }" @click="mode = 'create'">
            create
          </button>

          <button type="button" :class="{ active: mode === 'join' }" @click="mode = 'join'">
            join
          </button>
        </nav>

        <p v-if="errorMessage" class="error" role="alert">
          {{ errorMessage }}
        </p>

        <form @submit.prevent="submitForm">
          <div v-if="mode === 'join'" class="field">
            <label for="room-code">room code</label>

            <input
              id="room-code"
              v-model.trim="roomCode"
              class="room-code"
              type="text"
              maxlength="5"
              pattern="[A-Za-z0-9]{5}"
              autocomplete="off"
              placeholder="ABCDE"
              required
            />
          </div>

          <div class="field">
            <label for="display-name">display name</label>

            <input
              id="display-name"
              v-model.trim="activeName"
              type="text"
              maxlength="24"
              autocomplete="nickname"
              placeholder="your name"
              required
            />
          </div>

          <button class="submit-button" type="submit" :disabled="isCreatingRoom">
            <span>
              {{ mode === 'create' ? (isCreatingRoom ? 'creating' : 'create room') : 'enter room' }}
            </span>

            <span aria-hidden="true">↗</span>
          </button>
        </form>
      </div>
    </section>

    <footer class="footer">
      <p>rooms expire after inactivity</p>
      <p>no account / no history</p>
    </footer>
  </main>
</template>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  width: min(100% - 3rem, var(--page-width));
  min-height: 100vh;
  margin: 0 auto;
  padding: 2.5rem 0 2rem;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-inline: -1.5rem;
  padding: 0 1.5rem 1.25rem;
  border-bottom: 1px solid var(--color-border-soft);
}

.wordmark,
.status {
  margin: 0;
}

.wordmark {
  color: var(--color-text);
  font-size: 0.68rem;
  font-weight: 700;
  letter-spacing: 0.34em;
}

.status {
  color: var(--color-text-muted);
  font-size: 0.62rem;
  letter-spacing: 0.14em;
}

.content {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 310px;
  gap: 5rem;
  align-items: end;
  flex: 1;
  padding: 7rem 0 4rem;
}

.intro {
  align-self: start;
}

h1 {
  max-width: 7.5ch;
  margin: 0;
  color: var(--color-text);
  font-size: clamp(3.5rem, 9vw, 6.5rem);
  font-weight: 300;
  letter-spacing: -0.075em;
  line-height: 0.86;
}

h1 span {
  display: block;
}

.intro p {
  max-width: 24rem;
  margin: 2.5rem 0 0;
  color: var(--color-text-muted);
  font-size: 0.88rem;
  line-height: 1.75;
}

.panel {
  width: 100%;
  transform: translateY(-2rem);
}

.mode-switch {
  display: flex;
  gap: 1.75rem;
  margin-bottom: 3rem;
  border-bottom: 1px solid var(--color-border-soft);
}

.mode-switch button {
  position: relative;
  padding: 0 0 0.85rem;
  color: var(--color-text-inactive);
  background: transparent;
  border: 0;
  cursor: pointer;
  font-size: 0.72rem;
  letter-spacing: 0.04em;
}

.mode-switch button.active {
  color: var(--color-text);
}

.mode-switch button.active::after {
  position: absolute;
  right: 0;
  bottom: -1px;
  left: 0;
  height: 1px;
  background: var(--color-focus);
  content: '';
}

form {
  display: grid;
  gap: 2.4rem;
}

.field {
  display: grid;
  gap: 0.7rem;
}

label {
  color: var(--color-text-muted);
  font-size: 0.66rem;
  letter-spacing: 0.06em;
}

input {
  width: 100%;
  padding: 0.35rem 0 1rem;
  color: var(--color-text);
  background: transparent;
  border: 0;
  border-bottom: 1px solid var(--color-border);
  border-radius: 0;
  outline: none;
  font-size: 0.95rem;
}

input::placeholder {
  color: var(--color-text-dim);
}

input:focus {
  border-bottom-color: var(--color-focus);
}

.room-code {
  letter-spacing: 0.18em;
  text-transform: uppercase;
}

.submit-button {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 0.25rem;
  padding: 1rem 0;
  color: var(--color-text);
  background: transparent;
  border: 0;
  border-top: 1px solid var(--color-border);
  border-bottom: 1px solid var(--color-border);
  cursor: pointer;
  font-size: 0.72rem;
  letter-spacing: 0.04em;
}

.submit-button:hover:not(:disabled) {
  color: #ffffff;
  border-color: var(--color-focus);
}

.submit-button:disabled {
  cursor: wait;
  opacity: 0.45;
}

.error {
  margin: 0 0 2rem;
  color: var(--color-error);
  font-size: 0.8rem;
  line-height: 1.5;
}

.footer {
  display: flex;
  justify-content: space-between;
  padding-top: 1rem;
  color: var(--color-text-dim);
  border-top: 1px solid var(--color-border-faint);
  font-size: 0.58rem;
  letter-spacing: 0.08em;
}

.footer p {
  margin: 0;
}

@media (max-width: 680px) {
  .page {
    width: min(100% - 2rem, var(--page-width));
    padding-top: 1.5rem;
  }

  .header {
    margin-inline: 0;
    padding-inline: 0;
  }

  .status {
    display: none;
  }

  .content {
    display: block;
    padding: 5rem 0 4rem;
  }

  h1 {
    font-size: clamp(3.4rem, 18vw, 5rem);
  }

  .intro p {
    margin-top: 2rem;
  }

  .panel {
    margin-top: 6rem;
    transform: none;
  }

  .footer {
    display: grid;
    gap: 0.6rem;
  }
}
</style>
