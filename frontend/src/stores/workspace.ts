import { defineStore } from 'pinia'
import { workspaceApi, type CreateWorkspaceParams, type Workspace } from '../api/workspace'

export const useWorkspaceStore = defineStore('workspace', {
  state: () => ({
    list: [] as Workspace[],
    loaded: false
  }),
  getters: {
    currentId(): number {
      return Number(localStorage.getItem('novafs_workspace') || '1')
    },
    current(state): Workspace | null {
      return state.list.find((w) => w.id === this.currentId) || null
    }
  },
  actions: {
    /** 拉取工作空间列表；若当前 ID 不在列表中则自动切到第一个 */
    async load() {
      const list = await workspaceApi.list()
      this.list = list
      this.loaded = true
      if (list.length > 0 && !list.some((w) => w.id === this.currentId)) {
        this.switchTo(list[0].id)
      }
    },
    switchTo(id: number) {
      localStorage.setItem('novafs_workspace', String(id))
    },
    async create(params: CreateWorkspaceParams) {
      const ws = await workspaceApi.create(params)
      await this.load()
      this.switchTo(ws.id)
      return ws
    }
  }
})
