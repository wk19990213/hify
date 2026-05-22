export interface LayoutPosition {
  x: number
  y: number
}

/**
 * 基于拓扑排序的分层布局算法。
 * 已有位置数据的节点保持原位，无位置数据的节点自动布局。
 */
export function autoLayout(
  nodes: { index: number; positionX?: number; positionY?: number }[],
  edges: { sourceNodeIndex: number; targetNodeIndex: number }[]
): Map<number, LayoutPosition> {
  const result = new Map<number, LayoutPosition>()
  const NX = 240
  const NY = 140

  // 分离有位置和无位置的节点
  const pending = new Set<number>()
  for (const n of nodes) {
    if (n.positionX != null && n.positionY != null) {
      result.set(n.index, { x: n.positionX, y: n.positionY })
    } else {
      pending.add(n.index)
    }
  }

  if (pending.size === 0) return result

  // 构建邻接表和入度
  const adj = new Map<number, number[]>()
  const indeg = new Map<number, number>()
  for (const n of nodes) {
    adj.set(n.index, [])
    indeg.set(n.index, 0)
  }
  for (const e of edges) {
    adj.get(e.sourceNodeIndex)?.push(e.targetNodeIndex)
    indeg.set(e.targetNodeIndex, (indeg.get(e.targetNodeIndex) || 0) + 1)
  }

  // 拓扑排序分层
  const layers: number[][] = []
  let queue: number[] = []
  for (const [id, d] of indeg) {
    if (d === 0) queue.push(id)
  }

  while (queue.length > 0) {
    layers.push(queue)
    const next: number[] = []
    for (const u of queue) {
      for (const v of adj.get(u) || []) {
        indeg.set(v, indeg.get(v)! - 1)
        if (indeg.get(v) === 0) next.push(v)
      }
    }
    queue = next
  }

  // 为每层中无位置的节点分配坐标
  for (let layerIdx = 0; layerIdx < layers.length; layerIdx++) {
    const pendingInLayer = layers[layerIdx].filter(id => pending.has(id))
    if (pendingInLayer.length === 0) continue

    // 计算该层已放置节点的 Y 坐标（取均值作为层 Y）
    let layerY = layerIdx * NY
    const placed = layers[layerIdx].filter(id => !pending.has(id))
    if (placed.length > 0) {
      layerY = 0
      for (const id of placed) {
        const pos = result.get(id)
        if (pos) layerY += pos.y
      }
      layerY /= placed.length
    }

    // 计算起始 X 使该层居中
    const totalWidth = (pendingInLayer.length - 1) * NX
    const startX = -totalWidth / 2

    for (let i = 0; i < pendingInLayer.length; i++) {
      result.set(pendingInLayer[i], { x: startX + i * NX, y: layerY })
    }
  }

  return result
}
