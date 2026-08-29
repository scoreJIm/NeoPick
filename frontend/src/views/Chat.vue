<template>
  <div class="chat-container">
    <!-- Conversation list sidebar -->
    <aside class="chat-sidebar">
      <h2>Messages</h2>
      <div v-if="conversations.length === 0" class="empty-conversations">
        No conversations yet. Start by booking a lesson or contacting a teacher.
      </div>
      <div
        v-for="conv in conversations"
        :key="conv.id"
        class="conversation-item"
        :class="{ active: activeConversationId === conv.id }"
        @click="selectConversation(conv)"
      >
        <div class="conv-avatar">{{ (conv.teacherId || conv.studentId || '?').toString().charAt(0) }}</div>
        <div class="conv-info">
          <div class="conv-name">{{ conv.teacherId ? 'Teacher #' + conv.teacherId : 'Student #' + conv.studentId }}</div>
          <div class="conv-last-msg">{{ conv.lastMessageContent || 'No messages yet' }}</div>
        </div>
        <div v-if="conv.lastMessageAt" class="conv-time">{{ formatTime(conv.lastMessageAt) }}</div>
      </div>
    </aside>

    <!-- Main chat area -->
    <div class="chat-main" v-if="activeConversationId">
      <div class="chat-header">
        <span>Conversation</span>
        <span v-if="partnerTyping" class="typing-indicator">typing...</span>
      </div>

      <!-- Messages area -->
      <div class="messages-area" ref="messagesArea">
        <div v-if="messages.length === 0" class="empty-messages">
          Start the conversation by sending a message below.
        </div>
        <div
          v-for="msg in messages"
          :key="msg.id"
          class="message-row"
          :class="{ sent: msg.senderId === userId, received: msg.senderId !== userId }"
        >
          <div class="message-bubble" :class="{ sent: msg.senderId === userId }">
            <div class="message-text">{{ msg.content }}</div>
            <div class="message-meta">
              <span class="message-time">{{ formatTime(msg.sentAt) }}</span>
              <span v-if="msg.senderId === userId" class="read-receipt">
                {{ msg.read ? '✓✓' : '✓' }}
              </span>
            </div>
          </div>
        </div>
      </div>

      <!-- Input area -->
      <div class="input-area">
        <input
          v-model="newMessage"
          placeholder="Type a message..."
          maxlength="5000"
          @keyup.enter="sendMessage"
          @input="onTyping"
        />
        <button @click="sendMessage" :disabled="!newMessage.trim()">Send</button>
      </div>
    </div>

    <!-- No conversation selected state -->
    <div class="chat-main empty" v-else>
      <div class="empty-state">
        <p>Select a conversation from the sidebar to start chatting.</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted, onBeforeUnmount } from 'vue'
import SockJS from 'sockjs-client'
import { Stomp } from '@stomp/stompjs'
import api from '../api/client.js'

// State
const conversations = ref([])
const messages = ref([])
const activeConversationId = ref(null)
const newMessage = ref('')
const userId = ref(localStorage.getItem('userId') || '')
const partnerTyping = ref(false)
const stompClient = ref(null)
const messagesArea = ref(null)
let typingTimer = null

// Load conversations from REST API
async function loadConversations() {
  try {
    const { data } = await api.get('/conversations')
    conversations.value = data.data || data || []
  } catch (err) {
    console.error('Failed to load conversations:', err)
  }
}

// Load messages for a conversation via REST API
async function loadMessages(conversationId) {
  try {
    const { data } = await api.get(`/conversations/${conversationId}/messages`)
    messages.value = (data.data || data || []).reverse()
  } catch (err) {
    console.error('Failed to load messages:', err)
    messages.value = []
  }
}

// Select a conversation
async function selectConversation(conv) {
  activeConversationId.value = conv.id
  await loadMessages(conv.id)
  await nextTick()
  scrollToBottom()
}

// Connect to WebSocket via SockJS + STOMP
function connectWebSocket() {
  const token = localStorage.getItem('accessToken')
  if (!token) {
    console.warn('No access token, skipping WebSocket connection')
    return
  }

  const socket = new SockJS('/ws')
  const client = Stomp.over(socket)

  // Disable debug logging in production
  client.debug = () => {}

  client.connect(
    { Authorization: 'Bearer ' + token },
    () => {
      console.log('WebSocket connected')
      stompClient.value = client

      // Subscribe to own message queue for delivery confirmations
      client.subscribe('/user/queue/chat/' + activeConversationId.value, (msg) => {
        const body = JSON.parse(msg.body)
        handleIncomingMessage(body)
      })

      // Subscribe to typing indicators
      if (activeConversationId.value) {
        subscribeToTyping(activeConversationId.value)
      }

      // Subscribe to read receipts
      client.subscribe('/topic/chat/' + activeConversationId.value + '/read', (msg) => {
        const body = JSON.parse(msg.body)
        handleReadReceipt(body)
      })
    },
    (error) => {
      console.error('WebSocket connection error:', error)
    }
  )
}

function subscribeToTyping(conversationId) {
  if (!stompClient.value) return
  stompClient.value.subscribe(
    '/user/queue/chat/' + conversationId + '/typing',
    (msg) => {
      const body = JSON.parse(msg.body)
      partnerTyping.value = body.typing === true
      if (body.typing) {
        setTimeout(() => { partnerTyping.value = false }, 3000)
      }
    }
  )
}

function handleIncomingMessage(body) {
  // Avoid duplicates
  const exists = messages.value.some((m) => m.id === body.id)
  if (!exists) {
    messages.value.push(body)
    scrollToBottom()
  }
}

function handleReadReceipt(body) {
  // Mark messages from us as read based on readerId matching our userId
  messages.value.forEach((m) => {
    if (m.senderId === userId.value && !m.read) {
      m.read = true
    }
  })
}

// Send a message via STOMP
function sendMessage() {
  const content = newMessage.value.trim()
  if (!content || !stompClient.value || !stompClient.value.connected) return
  if (!activeConversationId.value) return

  // Determine recipient from the conversation
  const conv = conversations.value.find((c) => c.id === activeConversationId.value)
  const recipientId = getRecipientId(conv)

  stompClient.value.send(
    '/app/chat/' + activeConversationId.value,
    { 'content-type': 'application/json' },
    JSON.stringify({ content, recipientId })
  )

  newMessage.value = ''
  clearTyping()
}

// Typing indicator
function onTyping() {
  if (!stompClient.value || !stompClient.value.connected) return
  if (!activeConversationId.value) return

  if (typingTimer) clearTimeout(typingTimer)

  const conv = conversations.value.find((c) => c.id === activeConversationId.value)
  const recipientId = getRecipientId(conv)

  stompClient.value.send(
    '/app/chat/' + activeConversationId.value + '/typing',
    { 'content-type': 'application/json' },
    JSON.stringify({ typing: true, recipientId })
  )

  typingTimer = setTimeout(() => clearTyping(), 3000)
}

function clearTyping() {
  if (typingTimer) {
    clearTimeout(typingTimer)
    typingTimer = null
  }
  if (stompClient.value && stompClient.value.connected && activeConversationId.value) {
    const conv = conversations.value.find((c) => c.id === activeConversationId.value)
    const recipientId = getRecipientId(conv)
    stompClient.value.send(
      '/app/chat/' + activeConversationId.value + '/typing',
      { 'content-type': 'application/json' },
      JSON.stringify({ typing: false, recipientId })
    )
  }
}

function getRecipientId(conv) {
  if (!conv) return null
  // The conversation has studentId and teacherId; the other party is the recipient
  return conv.studentId === userId.value ? conv.teacherId?.toString() : conv.studentId
}

// Send read receipt
function sendReadReceipt() {
  if (!stompClient.value || !stompClient.value.connected) return
  if (!activeConversationId.value) return
  stompClient.value.send(
    '/app/chat/' + activeConversationId.value + '/read',
    {}
  )
}

function scrollToBottom() {
  nextTick(() => {
    const el = messagesArea.value
    if (el) el.scrollTop = el.scrollHeight
  })
}

function formatTime(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  const pad = (n) => String(n).padStart(2, '0')
  return pad(d.getHours()) + ':' + pad(d.getMinutes())
}

// Lifecycle
onMounted(async () => {
  await loadConversations()
  connectWebSocket()
})

onBeforeUnmount(() => {
  clearTyping()
  if (stompClient.value) {
    stompClient.value.disconnect()
    stompClient.value = null
  }
})
</script>

<style scoped>
.chat-container {
  display: flex;
  height: calc(100vh - 100px);
  border: 1px solid #eee;
  border-radius: 8px;
  overflow: hidden;
  margin-top: 0;
}

.chat-sidebar {
  width: 280px;
  border-right: 1px solid #eee;
  display: flex;
  flex-direction: column;
  background: #fafafa;
}

.chat-sidebar h2 {
  padding: 1rem;
  font-size: 1rem;
  font-weight: 600;
  border-bottom: 1px solid #eee;
}

.empty-conversations {
  padding: 2rem 1rem;
  color: #999;
  font-size: 0.85rem;
  text-align: center;
}

.conversation-item {
  display: flex;
  align-items: center;
  padding: 0.75rem 1rem;
  cursor: pointer;
  gap: 0.5rem;
  border-bottom: 1px solid #f0f0f0;
}

.conversation-item:hover,
.conversation-item.active {
  background: #eef;
}

.conv-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: #e0e0e0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.9rem;
  font-weight: 600;
  flex-shrink: 0;
}

.conv-info {
  flex: 1;
  min-width: 0;
}

.conv-name {
  font-size: 0.85rem;
  font-weight: 500;
}

.conv-last-msg {
  font-size: 0.75rem;
  color: #999;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.conv-time {
  font-size: 0.7rem;
  color: #bbb;
  flex-shrink: 0;
}

.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #fff;
}

.chat-main.empty {
  justify-content: center;
  align-items: center;
}

.empty-state {
  color: #999;
  font-size: 0.9rem;
}

.chat-header {
  padding: 0.75rem 1rem;
  border-bottom: 1px solid #eee;
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.typing-indicator {
  font-size: 0.8rem;
  color: #999;
  font-style: italic;
}

.messages-area {
  flex: 1;
  overflow-y: auto;
  padding: 1rem;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.empty-messages {
  color: #999;
  font-size: 0.85rem;
  text-align: center;
  margin-top: 2rem;
}

.message-row {
  display: flex;
}

.message-row.sent {
  justify-content: flex-end;
}

.message-row.received {
  justify-content: flex-start;
}

.message-bubble {
  max-width: 65%;
  padding: 0.6rem 0.8rem;
  border-radius: 12px;
  font-size: 0.9rem;
  line-height: 1.4;
}

.message-bubble.sent {
  background: #333;
  color: #fff;
  border-bottom-right-radius: 4px;
}

.message-bubble:not(.sent) {
  background: #f0f0f0;
  color: #222;
  border-bottom-left-radius: 4px;
}

.message-meta {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 4px;
  margin-top: 4px;
}

.message-time {
  font-size: 0.7rem;
  opacity: 0.7;
}

.read-receipt {
  font-size: 0.7rem;
  opacity: 0.7;
}

.input-area {
  display: flex;
  padding: 0.75rem 1rem;
  border-top: 1px solid #eee;
  gap: 0.5rem;
}

.input-area input {
  flex: 1;
  padding: 0.6rem;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 0.9rem;
  outline: none;
}

.input-area input:focus {
  border-color: #666;
}

.input-area button {
  padding: 0.6rem 1.2rem;
  background: #333;
  color: #fff;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.85rem;
}

.input-area button:disabled {
  background: #ccc;
  cursor: not-allowed;
}
</style>
