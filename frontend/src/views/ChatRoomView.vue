<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { useChatRoom } from '../composables/useChatRoom'
import {
  getDisplayNameError,
  isValidRoomCode,
  normaliseDisplayName,
  normaliseRoomCode,
} from '../validation'

const route = useRoute()
const router = useRouter()

const roomCode = normaliseRoomCode(route.params.code)
const displayName = normaliseDisplayName(route.query.name)

const draftMessage = ref('')
const messageViewport = ref(null)

const { connectionStatus, errorMessage, messages, participants, connect, disconnect, sendMessage } =
  useChatRoom(roomCode, displayName)

function scrollToLatestMessage() {
  nextTick(() => {
    if (messageViewport.value) {
      messageViewport.value.scrollTop = messageViewport.value.scrollHeight
    }
  })
}

function submitMessage() {
  if (sendMessage(draftMessage.value)) {
    draftMessage.value = ''
  }
}

function leaveRoom() {
  disconnect()

  router.push({
    name: 'home',
  })
}

function isSystemMessage(message) {
  return message.type !== 'CHAT_MESSAGE'
}

watch(() => messages.value.length, scrollToLatestMessage)

watch([connectionStatus, errorMessage], ([status, message]) => {
  if (status !== 'disconnected' || !message) {
    return
  }

  router.replace({
    name: 'home',
    query: {
      error: message,
    },
  })
})

onMounted(() => {
  if (!isValidRoomCode(roomCode) || getDisplayNameError(displayName)) {
    router.replace({
      name: 'home',
      query: {
        error: 'A room code and display name are required.',
      },
    })

    return
  }

  connect()
})

onBeforeUnmount(() => {
  disconnect('Page closed')
})
</script>

<template>
  <main class="chat-page">
    <header class="header">
      <RouterLink class="wordmark" :to="{ name: 'home' }"> LIMINAL </RouterLink>

      <p class="tagline">temporary private chat</p>
    </header>

    <section class="chat-layout">
      <div class="conversation">
        <p v-if="errorMessage" class="error" role="alert">
          {{ errorMessage }}
        </p>

        <div
          ref="messageViewport"
          class="message-list"
          aria-live="polite"
          aria-label="Room messages"
        >
          <p v-if="messages.length === 0" class="empty-state">Nothing has been said yet.</p>

          <template
            v-for="(message, index) in messages"
            :key="`${index}-${message.sender}-${message.content}`"
          >
            <div v-if="isSystemMessage(message)" class="system-message">
              <span>— {{ message.content }}</span>
              <time>{{ message.time }}</time>
            </div>

            <article v-else class="message">
              <div class="message-meta">
                <span>{{ message.sender }}</span>
                <time>{{ message.time }}</time>
              </div>

              <p class="message-body">
                {{ message.content }}
              </p>
            </article>
          </template>
        </div>
      </div>

      <aside class="room-panel">
        <div class="room-detail">
          <p class="section-label">room</p>
          <p class="room-code">{{ roomCode }}</p>
        </div>

        <div class="room-detail">
          <p class="section-label">present as</p>
          <p class="participant-name">{{ displayName }}</p>
        </div>

        <div class="room-detail participant-detail">
          <p class="section-label">in room</p>

          <ul class="participant-list" aria-label="Room participants">
            <li v-for="(participant, index) in participants" :key="`${participant}-${index}`">
              {{ participant }}
            </li>
          </ul>
        </div>

        <div class="room-detail">
          <p class="section-label">status</p>
          <p class="status" :data-status="connectionStatus">
            {{ connectionStatus }}
          </p>
        </div>

        <div class="room-detail">
          <p class="section-label">temporary</p>
          <p class="room-note">This room disappears after one hour without activity.</p>
        </div>
      </aside>
    </section>

    <footer class="room-footer">
      <form class="composer" @submit.prevent="submitMessage">
        <label class="visually-hidden" for="message"> Write a message </label>

        <input
          id="message"
          v-model="draftMessage"
          type="text"
          maxlength="500"
          placeholder="write a message"
          autocomplete="off"
          :disabled="connectionStatus !== 'connected'"
        />

        <button
          type="submit"
          aria-label="Send message"
          :disabled="connectionStatus !== 'connected' || !draftMessage.trim()"
        >
          ↗
        </button>
      </form>

      <button class="leave-button" type="button" @click="leaveRoom">
        <span>leave room</span>
        <span aria-hidden="true">↗</span>
      </button>
    </footer>
  </main>
</template>

<style scoped>
.chat-page {
  display: flex;
  flex-direction: column;
  width: min(100% - 4rem, 1180px);
  height: 100vh;
  margin: 0 auto;
  padding: 2.5rem 0 2rem;
  overflow: hidden;
}

.header {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 1.5rem;
  border-bottom: 1px solid var(--color-border-soft);
}

.wordmark {
  color: var(--color-text);
  font-size: 0.68rem;
  font-weight: 700;
  letter-spacing: 0.34em;
  text-decoration: none;
}

.tagline {
  margin: 0;
  color: var(--color-text-muted);
  font-size: 0.62rem;
  letter-spacing: 0.1em;
}

.chat-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 230px;
  gap: 4rem;
  flex: 1 1 auto;
  min-height: 0;
  padding: 3rem 0 2.25rem;
  overflow: hidden;
}

.conversation {
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
}

.error {
  flex: 0 0 auto;
  margin: 0 0 1.5rem;
  color: var(--color-error);
  font-size: 0.78rem;
  line-height: 1.5;
}

.message-list {
  display: flex;
  flex: 1 1 auto;
  flex-direction: column;
  align-items: flex-start;
  min-height: 0;
  padding: 0 1.2rem 1rem 0;
  overflow-x: hidden;
  overflow-y: auto;
  scrollbar-color: rgba(196, 216, 215, 0.18) transparent;
  scrollbar-gutter: stable;
  scrollbar-width: thin;
}

.message-list::-webkit-scrollbar {
  width: 4px;
}

.message-list::-webkit-scrollbar-track {
  background: transparent;
}

.message-list::-webkit-scrollbar-thumb {
  background: rgba(196, 216, 215, 0.18);
}

.message-list::-webkit-scrollbar-thumb:hover {
  background: rgba(196, 216, 215, 0.3);
}

.empty-state {
  width: min(100%, 42rem);
  margin: 0;
  color: var(--color-text-dim);
  font-size: 0.78rem;
}

.message {
  flex: 0 0 auto;
  width: min(100%, 42rem);
  margin: 0 0 2.4rem;
}

.message-meta {
  display: flex;
  align-items: baseline;
  gap: 0.8rem;
  margin-bottom: 0.7rem;
  color: var(--color-text-dim);
  font-size: 0.62rem;
  letter-spacing: 0.05em;
}

.message-meta time {
  opacity: 0.75;
}

.message-body {
  max-width: 36rem;
  margin: 0;
  color: var(--color-text);
  font-size: 1rem;
  line-height: 1.6;
  overflow-wrap: anywhere;
}

.system-message {
  display: flex;
  flex: 0 0 auto;
  align-items: baseline;
  gap: 0.8rem;
  width: min(100%, 42rem);
  margin: 0 0 2.4rem;
  color: var(--color-text-dim);
  font-size: 0.68rem;
  letter-spacing: 0.04em;
}

.system-message time {
  font-size: 0.62rem;
  opacity: 0.75;
}

.room-panel {
  min-height: 0;
  overflow-y: auto;
  scrollbar-width: none;
}

.room-panel::-webkit-scrollbar {
  display: none;
}

.room-detail {
  padding-bottom: 1.7rem;
  margin-bottom: 1.7rem;
  border-bottom: 1px solid var(--color-border-faint);
}

.section-label {
  margin: 0;
  color: var(--color-text-dim);
  font-size: 0.62rem;
  letter-spacing: 0.1em;
}

.room-code,
.participant-name,
.status,
.room-note,
.participant-list {
  margin: 0.8rem 0 0;
}

.room-code {
  color: var(--color-text);
  font-size: 0.9rem;
  letter-spacing: 0.2em;
}

.participant-name {
  color: var(--color-text);
  font-size: 1rem;
}

.participant-list {
  padding: 0;
  color: var(--color-text-muted);
  font-size: 0.78rem;
  line-height: 1.8;
  list-style: none;
}

.status {
  color: var(--color-text-muted);
  font-size: 0.88rem;
}

.status[data-status='connection error'],
.status[data-status='disconnected'] {
  color: var(--color-error);
}

.room-note {
  color: var(--color-text-muted);
  font-size: 0.76rem;
  line-height: 1.65;
}

.room-footer {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 230px;
  gap: 4rem;
  flex: 0 0 auto;
  border-top: 1px solid var(--color-border);
}

.composer {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
}

.composer input {
  min-width: 0;
  padding: 1.45rem 0;
  color: var(--color-text);
  background: transparent;
  border: 0;
  border-radius: 0;
  outline: none;
  font-size: 0.9rem;
}

.composer input::placeholder {
  color: var(--color-text-dim);
}

.composer input:disabled {
  opacity: 0.5;
}

.composer button,
.leave-button {
  color: var(--color-text-muted);
  background: transparent;
  border: 0;
  cursor: pointer;
}

.composer button {
  align-self: stretch;
  min-width: 3rem;
  padding: 0;
  font-size: 1rem;
}

.leave-button {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding: 0;
  font-size: 0.7rem;
  letter-spacing: 0.04em;
}

.composer button:hover:not(:disabled),
.leave-button:hover {
  color: var(--color-text);
}

.composer button:disabled {
  cursor: default;
  opacity: 0.3;
}

.visually-hidden {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}

@media (max-width: 820px) {
  .chat-page {
    width: min(100% - 2rem, 1180px);
    padding-top: 1.5rem;
  }

  .chat-layout {
    display: flex;
    flex-direction: column;
    gap: 2rem;
    padding-top: 2.5rem;
  }

  .conversation {
    flex: 1 1 auto;
  }

  .room-panel {
    order: -1;
    display: grid;
    flex: 0 0 auto;
    grid-template-columns: 1fr 1fr;
    gap: 1.5rem;
    overflow: visible;
  }

  .room-detail {
    margin: 0;
  }

  .room-detail:nth-child(n + 3) {
    display: none;
  }

  .room-footer {
    grid-template-columns: minmax(0, 1fr) 150px;
    gap: 2rem;
  }
}

@media (max-width: 520px) {
  .chat-page {
    width: min(100% - 1.5rem, 1180px);
    padding-bottom: 1rem;
  }

  .tagline {
    font-size: 0.58rem;
  }

  .chat-layout {
    gap: 1.5rem;
    padding-top: 2rem;
    padding-bottom: 1.5rem;
  }

  .room-panel {
    grid-template-columns: 1fr 1fr;
    gap: 1rem;
  }

  .room-detail {
    padding-bottom: 1rem;
  }

  .message-list {
    padding-right: 0.75rem;
  }

  .message,
  .system-message,
  .empty-state {
    width: 100%;
  }

  .message-body {
    max-width: 100%;
  }

  .room-footer {
    display: block;
  }

  .composer {
    border-bottom: 1px solid var(--color-border-soft);
  }

  .leave-button {
    padding: 1rem 0 0;
  }
}
</style>
