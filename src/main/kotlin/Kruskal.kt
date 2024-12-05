class UnionFind(n: Int) {
    private val parent: IntArray = IntArray(n) { it }
    private val size: IntArray = IntArray(n) { 1 }

    private fun find(x: Int): Int {
        if (parent[x] != x) {
            parent[x] = find(parent[x])
        }
        return parent[x]
    }

    fun union(x: Int, y: Int): Boolean {
        val rootX = find(x)
        val rootY = find(y)
        if (rootX == rootY) {
            return false
        }
        if (size[rootX] < size[rootY]) {
            parent[rootX] = rootY
            size[rootY] += size[rootX]
        } else {
            parent[rootY] = rootX
            size[rootX] += size[rootY]
        }
        return true
    }
}

data class Edge(val u: Int, val v: Int, val weight: Int)

fun kruskal(n: Int, edges: List<Edge>): List<Edge> {
    val uf = UnionFind(n)
    val sortedEdges = edges.sortedBy { it.weight }
    val mst = mutableListOf<Edge>()
    for (edge in sortedEdges) {
        if (uf.union(edge.u, edge.v)) {
            mst.add(edge)
            if (mst.size == n - 1) {
                break
            }
        }
    }
    return mst
}

fun main() {
    val n = 5
    val edges = listOf(
        Edge(0, 1, 2),
        Edge(0, 3, 6),
        Edge(1, 2, 3),
        Edge(1, 3, 8),
        Edge(1, 4, 5),
        Edge(2, 4, 7),
        Edge(3, 4, 9),
    )
    val mst = kruskal(n, edges)
    for (edge in mst) {
        println("(${edge.u}, ${edge.v}) - ${edge.weight}")
    }
}
