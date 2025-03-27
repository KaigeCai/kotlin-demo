import kotlin.random.Random

fun generateUniqueRandomNumbersShuffled(): List<Int> {
    // 创建1~508的数字列表
    val numbers = (1..508).toMutableList()
    // 洗牌前5个数字
    for (i in 0 until 5) {
        val j = Random.nextInt(i, numbers.size)
        numbers[i] = numbers[j].also { numbers[j] = numbers[i] }
    }
    // 取前5个
    return numbers.take(5)
}

fun generateUniqueRandomNumbers(): List<Int> {
    // 使用 HashSet 来存储不重复的数字
    val numbers = HashSet<Int>()
    // 当集合大小小于5时，继续生成随机数
    while (numbers.size < 5) {
        numbers.add(Random.nextInt(1, 509)) // 生成1~508的随机数
    }
    // 转换为List返回
    return numbers.toList()
}

fun main() {
    val randomNumbers1 = generateUniqueRandomNumbers()
    println("生成的5个不重复随机数：")
    randomNumbers1.forEachIndexed { index, number ->
        println("第${index + 1}个数字: $number")
    }

    val randomNumbers2 = generateUniqueRandomNumbersShuffled()
    println("生成的5个不重复随机数(洗牌法)：")
    randomNumbers2.forEachIndexed { index, number ->
        println("第${index + 1}个数字: $number")
    }
}