<template>
  <div class="chat-page">
    <header class="chat-header">
      <h2>AI 问答</h2>
      <span class="sub">基于工作空间内文档回答问题</span>
    </header>

    <div class="chat-body" ref="bodyRef">
      <div class="chat-panel">
        <div v-if="messages.length === 0" class="empty">
          <div class="empty-icon">✦</div>
          <p class="empty-title">向你的文档提问</p>
          <p class="empty-sub">例如:"这份文档的核心结论是什么?"</p>
          <div class="suggestions">
            <el-tag v-for="s in suggestions" :key="s" class="suggest" effect="plain" @click="ask(s)">{{ s }}</el-tag>
          </div>
        </div>

        <div v-for="(m, i) in messages" :key="i" class="msg" :class="m.role">
          <template v-if="m.role === 'user'">
            <div class="q">{{ m.content }}</div>
          </template>
          <template v-else>
            <div class="a">
              <div class="answer">{{ m.content }}</div>
              <div v-if="m.sources?.length" class="sources">
                <div class="sources-title">引用来源 ({{ m.sources.length }})</div>
                <div v-for="(s, j) in m.sources" :key="j" class="source-card">
                  <div class="source-head">
                    <span class="doc-name">📄 {{ s.documentName }}</span>
                    <span class="score">{{ (s.score * 100).toFixed(1) }}% 相关</span>
                  </div>
                  <div class="source-content">{{ s.content }}</div>
                </div>
              </div>
            </div>
          </template>
        </div>
      </div>
    </div>

    <div class="chat-input">
      <el-input
        v-model="input"
        placeholder="输入问题,回车发送…"
        size="large"
        :disabled="loading"
        @keyup.enter="send"
      >
        <template #append>
          <el-button :loading="loading" @click="send">发送</el-button>
        </template>
      </el-input>
    </div>
  </div>
</template>

<script setup lang="ts">
import { nextTick, ref } from 'vue'
import { ragApi, type SearchResult } from '../api/rag'

interface ChatMessage {
  role: 'user' | 'assistant'
  content: string
  sources?: SearchResult[]
}

const workspaceId = () => localStorage.getItem('novafs_workspace') || '1'
const messages = ref<ChatMessage[]>([])
const input = ref('')
const loading = ref(false)
const bodyRef = ref<HTMLElement>()

const suggestions = ['文档里最重要的结论是什么？', '帮我总结一下核心要点', '文档中有哪些关键数据？']

async function send() {
  const q = input.value.trim()
  if (!q || loading.value) return
  input.value = ''
  messages.value.push({ role: 'user', content: q })
  loading.value = true
  scrollToBottom()
  try {
    const resp = await ragApi.chat(workspaceId(), q)
    messages.value.push({ role: 'assistant', content: resp.answer, sources: resp.sources })
  } catch {
    messages.value.push({ role: 'assistant', content: '抱歉,回答失败,请检查模型服务配置后重试。' })
  } finally {
    loading.value = false
    scrollToBottom()
  }
}

async function ask(s: string) {
  input.value = s
  await send()
}

function scrollToBottom() {
  nextTick(() => { bodyRef.value?.scrollTo({ top: bodyRef.value.scrollHeight, behavior: 'smooth' }) })
}
</script>

<style scoped>
.chat-page { height: 100%; display: flex; flex-direction: column; }
.chat-header { padding: 20px 28px 14px; }
.chat-header h2 { margin: 0 0 4px; font-size: 20px; color: var(--novafs-text); }
.chat-header .sub { font-size: 13px; color: var(--novafs-text-sub); }

.chat-body { flex: 1; overflow-y: auto; padding: 0 28px 16px; }

/* 白色工作台面板:与文档库卡片同一视觉语言 */
.chat-panel {
  background: #fff;
  border-radius: 16px;
  border: 1px solid var(--novafs-card-border);
  box-shadow: 0 2px 12px rgba(59, 157, 255, 0.06);
  padding: 24px 28px;
  min-height: 100%;
}

.empty { text-align: center; padding: 60px 0 30px; }
.empty-icon {
  width: 56px; height: 56px; margin: 0 auto 12px; border-radius: 16px;
  background: var(--novafs-gradient);
  color: #fff; font-size: 26px; display: flex; align-items: center; justify-content: center;
  box-shadow: 0 10px 24px rgba(59, 157, 255, 0.28);
}
.empty-title { font-size: 17px; font-weight: 700; color: var(--novafs-text); margin: 0; }
.empty-sub { font-size: 13px; color: var(--novafs-text-muted); margin: 6px 0 18px; }
.suggestions { display: flex; gap: 8px; justify-content: center; flex-wrap: wrap; }
.suggest { cursor: pointer; }

/* 消息:问题用实色块,回答用纯文本 + 来源卡片 */
.msg { margin-bottom: 18px; }
.q {
  display: inline-block;
  background: var(--novafs-primary);
  color: #fff;
  border-radius: 10px;
  padding: 10px 16px;
  font-size: 14px;
  line-height: 1.6;
}
.a .answer { color: var(--novafs-text); font-size: 14px; line-height: 1.8; white-space: pre-wrap; margin-top: 6px; }

.sources { margin-top: 14px; }
.sources-title { font-size: 12px; font-weight: 600; color: var(--novafs-text-sub); margin-bottom: 8px; }
.source-card {
  background: #f4f9ff;
  border: 1px solid var(--novafs-card-border);
  border-radius: 10px;
  padding: 10px 14px;
  margin-bottom: 8px;
}
.source-head { display: flex; justify-content: space-between; font-size: 12px; }
.doc-name { color: var(--novafs-primary); font-weight: 600; }
.score { color: #e59a2b; font-weight: 700; }
.source-content { font-size: 12px; color: #5a7aa6; margin-top: 4px; line-height: 1.6; }

.chat-input { padding: 14px 28px 20px; }
</style>