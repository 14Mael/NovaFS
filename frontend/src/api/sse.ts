/**
 * SSE 实时连接（fetch 流式实现）
 * <p>EventSource 无法携带 Authorization 请求头，Sa-Token 校验又必须带 token，
 * 因此使用 fetch + ReadableStream 手动解析 SSE 帧（event/data 块）。</p>
 */
export interface SseHandlers {
  /** 收到命名事件（如 upload-progress / upload-complete） */
  onEvent?: (event: string, data: unknown) => void
  /** 收到无事件名的默认 data 帧 */
  onData?: (data: unknown) => void
  /** 连接出错 */
  onError?: (err: Error) => void
  /** 连接关闭（正常或异常） */
  onClose?: () => void
}

/** 建立 SSE 连接，返回关闭函数 */
export function connectSse(handlers: SseHandlers): () => void {
  let closed = false
  let controller: AbortController | null = null

  async function run() {
    controller = new AbortController()
    const token = localStorage.getItem('novafs_token')
    try {
      const resp = await fetch('/api/sse/connect', {
        headers: token ? { Authorization: token } : {},
        signal: controller.signal
      })
      if (!resp.ok || !resp.body) {
        throw new Error(`SSE 连接失败(${resp.status})`)
      }
      const reader = resp.body.getReader()
      const decoder = new TextDecoder()
      let buffer = ''
      while (!closed) {
        const { done, value } = await reader.read()
        if (done) break
        buffer += decoder.decode(value, { stream: true })
        // SSE 帧以空行分隔，逐帧解析
        let sep: number
        while ((sep = buffer.indexOf('\n\n')) >= 0) {
          const frame = buffer.slice(0, sep)
          buffer = buffer.slice(sep + 2)
          parseFrame(frame, handlers)
        }
      }
    } catch (err) {
      if (!closed) {
        handlers.onError?.(err instanceof Error ? err : new Error(String(err)))
      }
    } finally {
      if (!closed) {
        handlers.onClose?.()
      }
    }
  }

  run()
  return () => {
    closed = true
    controller?.abort()
  }
}

function parseFrame(frame: string, handlers: SseHandlers) {
  let event = ''
  let data = ''
  for (const line of frame.split('\n')) {
    if (line.startsWith('event:')) {
      event = line.slice(6).trim()
    } else if (line.startsWith('data:')) {
      data += line.slice(5).trim()
    }
  }
  if (!data) return
  let payload: unknown = data
  try {
    payload = JSON.parse(data)
  } catch {
    /* 非 JSON 数据原样透传 */
  }
  if (event) {
    handlers.onEvent?.(event, payload)
  } else {
    handlers.onData?.(payload)
  }
}
