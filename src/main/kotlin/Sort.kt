// 冒泡排序
fun bubbleSort(arr: IntArray) {
    val n = arr.size // 获取数组的长度
    for (i in 0 until n - 1) { // 外层循环控制每一轮排序，比较相邻的两个元素，末尾元素不需要比较。
        for (j in 0 until n - 1 - i) { // 内层循环比较相邻的元素，每次减少比较范围，避免重复比较
            if (arr[j] > arr[j + 1]) { // 如果前一个元素大于后一个元素
                val temp = arr[j] // 将较大的元素存储到临时变量
                arr[j] = arr[j + 1] // 把较小的元素移到前面
                arr[j + 1] = temp // 把较大的元素移到后面
            }
        }
    }
}

fun main() {
    val arr = intArrayOf(99, 64, 34, 25, 12, 88, 11)
    println("排序前：${arr.joinToString()}")
    bubbleSort(arr)
    println("排序后：${arr.joinToString()}")
}
