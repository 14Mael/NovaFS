/// <reference lib="webworker" />
/**
 * MD5 计算 Worker：spark-md5 增量计算，不阻塞主线程
 * 主线程 postMessage({ file, chunkSize })；回传 progress / done / error
 */
import SparkMD5 from 'spark-md5'

self.onmessage = (e: MessageEvent<{ file: File; chunkSize: number }>) => {
  const { file, chunkSize } = e.data
  const spark = new SparkMD5.ArrayBuffer()
  let offset = 0

  const reader = new FileReader()
  reader.onerror = () => {
    postMessage({ type: 'error', message: '读取文件失败' })
  }
  reader.onload = (ev) => {
    spark.append(ev.target?.result as ArrayBuffer)
    offset += chunkSize
    postMessage({
      type: 'progress',
      percent: Math.min(99, Math.round((offset / file.size) * 100))
    })
    if (offset < file.size) {
      readNext()
    } else {
      postMessage({ type: 'done', md5: spark.end() })
    }
  }
  const readNext = () => {
    reader.readAsArrayBuffer(file.slice(offset, offset + chunkSize))
  }
  readNext()
}
