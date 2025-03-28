fun main() {
    val targetNumber = (1..100).random() // 随机生成1-100之间的数字
    var guess: Int

    println("欢迎玩猜数字游戏！请输入一个1-100之间的数字：")

    do {
        guess = readln().toInt() // 获取用户输入
        when {
            guess < targetNumber -> println("太小了，再试一次：")
            guess > targetNumber -> println("太大了，再试一次：")
            else -> println("恭喜你，猜对了！目标数字是 $targetNumber")
        }
    } while (guess != targetNumber)
}