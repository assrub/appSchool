import { ref, onMounted, onUnmounted } from 'vue'

export function useWebSocket(url) {
  const connected = ref(false)
  const lastMessage = ref(null)
  let ws = null
  let reconnectTimer = null
  let pingTimer = null
  let reconnectDelay = 1000 // Start at 1 second
  const maxDelay = 30000 // Max 30 seconds

  function connect() {
    try {
      ws = new WebSocket(url)

      ws.onopen = () => {
        connected.value = true
        reconnectDelay = 1000 // Reset delay on successful connection
        pingTimer = setInterval(() => {
          if (ws?.readyState === WebSocket.OPEN) ws.send('ping')
        }, 30000)
      }

      ws.onmessage = (event) => {
        if (event.data === 'pong') return
        try {
          lastMessage.value = JSON.parse(event.data)
        } catch {}
      }

      ws.onclose = () => {
        connected.value = false
        clearInterval(pingTimer)
        // Exponential backoff with jitter
        const jitter = Math.random() * 1000
        reconnectTimer = setTimeout(connect, reconnectDelay + jitter)
        reconnectDelay = Math.min(reconnectDelay * 2, maxDelay)
      }

      ws.onerror = () => {
        ws?.close()
      }
    } catch {
      const jitter = Math.random() * 1000
      reconnectTimer = setTimeout(connect, reconnectDelay + jitter)
      reconnectDelay = Math.min(reconnectDelay * 2, maxDelay)
    }
  }

  function disconnect() {
    clearInterval(pingTimer)
    clearTimeout(reconnectTimer)
    ws?.close()
  }

  onMounted(connect)
  onUnmounted(disconnect)

  return { connected, lastMessage, disconnect }
}
