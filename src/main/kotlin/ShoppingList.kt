fun main() {
    val shoppingList = mutableListOf<String>() // 可变列表存储购物项

    println("输入你要买的东西（输入 '结束' 退出）：")
    while (true) {
        val item = readln()
        if (item == "结束") break // 输入“结束”退出循环
        shoppingList.add(item)
        println("已添加: $item")
    }

    println("你的购物清单：")
    shoppingList.forEachIndexed { index, item -> println("${index + 1}. $item") }
}