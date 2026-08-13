/**
 * 我的文件页导航控制（供路由守卫拦截鼠标侧键后退）
 * <p>鼠标侧键的后退导航无法通过 preventDefault 阻止（浏览器不可取消默认行为），
 * 改为：侧键按下打标记 → 路由 beforeEach 拦截该次导航并执行"返回上级目录"。</p>
 */

/** FilesView 注册的"返回上级"回调（挂载时注册、卸载时清空） */
let upHandler: (() => void) | null = null

/** 侧键后退待拦截标记 */
let pendingBackBlock = false

export function registerFilesUp(handler: (() => void) | null) {
  upHandler = handler
}

export function getFilesUp(): (() => void) | null {
  return upHandler
}

export function setBackBlock() {
  pendingBackBlock = true
}

export function peekBackBlock(): boolean {
  return pendingBackBlock
}

export function clearBackBlock() {
  pendingBackBlock = false
}

/** 消费标记：true 表示此前设置了拦截且未被路由守卫消费（即无导航发生） */
export function consumeBackBlock(): boolean {
  const blocked = pendingBackBlock
  pendingBackBlock = false
  return blocked
}
