import { ref, onMounted, onUnmounted } from 'vue'

export function useWebSocket(url) {
  const connected = ref(false)
  const lastMessage = ref(null)
  let ws = null
  let reconnectTimer = null
  let pingTimer = null

  function connect() {
    try {
      ws = new WebSocket(url)

      ws.onopen = () => {
        connected.value = true
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
        reconnectTimer = setTimeout(connect, 5000)
      }

      ws.onerror = () => {
        ws?.close()
      }
    } catch {
      reconnectTimer = setTimeout(connect, 5000)
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
