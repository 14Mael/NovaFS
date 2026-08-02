import request from './request'

export interface RagDocument {
  id: number
  workspaceId: number
  userId: number
  name: string
  contentType: string
  size: number
  status: number // 0 解析中 1 已索引 2 失败
  chunkCount: number
  errorMsg: string | null
  createdAt: string
  updatedAt: string
}

export interface SearchResult {
  pointId: string
  documentId: number
  documentName: string
  score: number
  content: string
}

export interface ChatResponse {
  answer: string
  sources: SearchResult[]
}

export interface PageResult<T> {
  page: number
  pageSize: number
  total: number
  pages: number
  records: T[]
}

export const ragApi = {
  upload(workspaceId: number, file: File) {
    const form = new FormData()
    form.append('file', file)
    return request.post<RagDocument>(`/rag/documents?workspaceId=${workspaceId}`, form)
  },
  ingestText(workspaceId: number, name: string, content: string) {
    return request.post<RagDocument>('/rag/documents/text', { workspaceId, name, content })
  },
  page(workspaceId: number, page = 1, pageSize = 20) {
    return request.get<PageResult<RagDocument>>('/rag/documents', { params: { workspaceId, page, pageSize } })
  },
  remove(id: number) {
    return request.delete<null>(`/rag/documents/${id}`)
  },
  search(workspaceId: number, query: string, topK?: number) {
    return request.post<SearchResult[]>('/rag/search', { workspaceId, query, topK })
  },
  chat(workspaceId: number, question: string) {
    return request.post<ChatResponse>('/rag/chat', { workspaceId, question })
  }
}