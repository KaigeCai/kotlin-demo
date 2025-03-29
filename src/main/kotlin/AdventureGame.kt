import java.io.File
import java.util.*
import kotlin.random.Random
import kotlin.system.exitProcess

// 玩家类
class Player {
    var name: String = "冒险者"
    var health: Int = 100
    var gold: Int = 10
    var inventory: MutableList<String> = mutableListOf()
    var activeQuests: MutableList<String> = mutableListOf()
    var completedQuests: MutableList<String> = mutableListOf()

    fun showStatus() {
        println("玩家: $name | 生命值: $health | 金币: $gold")
        println("背包: ${inventory.joinToString(", ")}")
        if (activeQuests.isNotEmpty()) {
            println("进行中的任务: ${activeQuests.joinToString(", ")}")
        }
        if (completedQuests.isNotEmpty()) {
            println("已完成的任务: ${completedQuests.joinToString(", ")}")
        }
    }
}

// 地点类
class Location(val name: String, val description: String) {
    val exits: MutableMap<String, Location> = mutableMapOf()
    val items: MutableList<String> = mutableListOf()
    var hasVillager: Boolean = false
}

// 游戏类
class Game {
    private var player = Player()
    private var currentLocation: Location
    private val locations = mutableMapOf<String, Location>()
    private var isFirstTime = true

    init {
        val village = Location("村庄", "一个宁静的小村庄，村民们过着平静的生活。").apply {
            hasVillager = true
            items.add("苹果")
            items.add("面包")
        }
        val forest = Location("森林", "一片茂密的森林，隐藏着未知的危险。").apply {
            items.add("木剑")
            items.add("草药")
        }
        val cave = Location("洞穴", "一个阴暗的洞穴，传出奇怪的声音。").apply {
            items.add("矿石")
            items.add("火把")
        }
        val lake = Location("湖泊", "一个清澈的湖泊，周围风景优美。").apply {
            hasVillager = true  // 增加一个湖边村民
            items.add("鱼竿")
            items.add("鱼")
        }

        village.exits["北"] = forest
        village.exits["东"] = lake
        forest.exits["南"] = village
        forest.exits["东"] = cave
        cave.exits["西"] = forest
        lake.exits["西"] = village

        locations["村庄"] = village
        locations["森林"] = forest
        locations["洞穴"] = cave
        locations["湖泊"] = lake

        currentLocation = village
    }

    // 游戏主循环
    fun start(isLoaded: Boolean = false) {
        if (!isLoaded) {
            println("欢迎来到冒险游戏！请输入你的名字：")
            player.name = readlnOrNull()?.takeIf { it.isNotBlank() } ?: "冒险者"
        }
        println("你醒来发现自己在${currentLocation.name}。冒险开始了！")

        // 第一次进入游戏强制与村民对话
        if (isFirstTime && !isLoaded) {
            println("\n一位村民向你走来...")
            talkToVillager()
            isFirstTime = false
        }

        while (true) {
            println()
            player.showStatus()
            println("你当前在: ${currentLocation.name}")
            println(currentLocation.description)
            println("可去的方向: ${currentLocation.exits.keys.joinToString(", ")}")
            println("地上有: ${currentLocation.items.joinToString(", ")}")
            println("输入命令（方向、拾取、与村民对话、打开背包、保存、退出）：")
            when (val input = readlnOrNull()?.lowercase(Locale.getDefault())) {
                "北", "南", "东", "西" -> move(input)
                "拾取" -> pickUpItem()
                "与村民对话" -> talkToVillager()
                "打开背包" -> openInventory()
                "保存" -> saveGame()
                "退出" -> {
                    println("感谢游玩！")
                    break
                }

                else -> println("无效命令！")
            }

            // 随机事件 - 提高遇到村民的概率
            if (Random.nextInt(10) < 4) {  // 40%概率触发随机事件
                if (Random.nextBoolean() && currentLocation.name != "村庄") {
                    randomEvent()
                } else if (currentLocation.hasVillager) {
                    println("\n一位村民向你走来...")
                    talkToVillager()
                }
            }
        }
    }

    // 移动
    private fun move(direction: String) {
        val newLocation = currentLocation.exits[direction]
        if (newLocation != null) {
            currentLocation = newLocation
            println("你移动到了${currentLocation.name}。")
        } else {
            println("你不能往那个方向走！")
        }
    }

    // 拾取物品
    private fun pickUpItem() {
        if (currentLocation.items.isNotEmpty()) {
            val item = currentLocation.items.removeAt(0)
            player.inventory.add(item)
            println("你拾取了: $item")
        } else {
            println("这里没有可拾取的物品。")
        }
    }

    // 与村民对话 - 增加多个任务
    private fun talkToVillager() {
        if (currentLocation.hasVillager) {
            when (currentLocation.name) {
                "村庄" -> {
                    if (player.activeQuests.contains("寻找木剑") && player.inventory.contains("木剑")) {
                        println("村民说：你找到了木剑！太感谢了！这是你的奖励。")
                        player.inventory.remove("木剑")
                        player.gold += 15
                        player.activeQuests.remove("寻找木剑")
                        player.completedQuests.add("寻找木剑")
                    } else if (!player.completedQuests.contains("寻找木剑") && !player.activeQuests.contains("寻找木剑")) {
                        println("村民说：欢迎你，${player.name}！我需要一柄木剑，你能帮我从森林里找来吗？")
                        println("1. 接受任务 | 2. 拒绝")
                        val choice = readlnOrNull()
                        if (choice == "1") {
                            player.activeQuests.add("寻找木剑")
                            println("你接受了任务：寻找木剑")
                        } else {
                            println("村民说：好吧，也许下次吧。")
                        }
                    } else if (player.activeQuests.contains("寻找木剑")) {
                        println("村民说：你找到木剑了吗？它在森林里。")
                    }

                    // 第二个任务
                    if (player.completedQuests.contains("寻找木剑") && !player.completedQuests.contains("收集矿石")) {
                        println("铁匠说：我看到你帮助了村民，能帮我从洞穴收集一些矿石吗？")
                        println("1. 接受任务 | 2. 拒绝")
                        val choice = readlnOrNull()
                        if (choice == "1") {
                            player.activeQuests.add("收集矿石")
                            println("你接受了任务：收集矿石")
                        }
                    } else if (player.activeQuests.contains("收集矿石") && player.inventory.contains("矿石")) {
                        println("铁匠说：太好了，你带来了矿石！这是你的奖励。")
                        player.inventory.remove("矿石")
                        player.gold += 25
                        player.activeQuests.remove("收集矿石")
                        player.completedQuests.add("收集矿石")
                    }
                }

                "湖泊" -> {
                    if (!player.completedQuests.contains("钓鱼") && !player.activeQuests.contains("钓鱼")) {
                        println("渔夫说：你好啊，${player.name}！能帮我钓几条鱼吗？")
                        println("1. 接受任务 | 2. 拒绝")
                        val choice = readlnOrNull()
                        if (choice == "1") {
                            player.activeQuests.add("钓鱼")
                            println("你接受了任务：钓鱼")
                        }
                    } else if (player.activeQuests.contains("钓鱼") && player.inventory.contains("鱼")) {
                        println("渔夫说：哇，你钓到鱼了！太棒了！这是你的奖励。")
                        player.inventory.remove("鱼")
                        player.gold += 20
                        player.activeQuests.remove("钓鱼")
                        player.completedQuests.add("钓鱼")
                    } else if (player.activeQuests.contains("钓鱼")) {
                        println("渔夫说：用鱼竿在湖边钓鱼就能抓到鱼。")
                    }
                }
            }
        } else {
            println("这里没有村民可以对话。")
        }
    }

    // 随机事件（战斗）
    private fun randomEvent() {
        if (!currentLocation.hasVillager) {
            val enemies = listOf("野狼", "毒蜘蛛", "强盗")
            val enemy = enemies.random()
            println("一只${enemy}突然跳了出来！")
            var enemyHealth = when (enemy) {
                "野狼" -> 20
                "毒蜘蛛" -> 15
                "强盗" -> 30
                else -> 20
            }

            while (enemyHealth > 0 && player.health > 0) {
                println("${enemy}生命值: $enemyHealth | 你的生命值: ${player.health}")
                println("1. 攻击 | 2. 逃跑")
                val choice = readlnOrNull()
                if (choice == "1") {
                    val baseDamage = when {
                        player.inventory.contains("木剑") -> 10
                        else -> 5
                    }
                    val damage = Random.nextInt(baseDamage, baseDamage + 10)
                    enemyHealth -= damage
                    println("你对${enemy}造成了 $damage 点伤害！")
                    if (enemyHealth > 0) {
                        val enemyDamage = when (enemy) {
                            "野狼" -> Random.nextInt(5, 10)
                            "毒蜘蛛" -> Random.nextInt(8, 13)
                            "强盗" -> Random.nextInt(10, 15)
                            else -> Random.nextInt(5, 10)
                        }
                        player.health -= enemyDamage
                        println("${enemy}对你造成了 $enemyDamage 点伤害！")
                    }
                } else {
                    if (Random.nextBoolean()) {
                        println("你成功逃跑了！")
                        break
                    } else {
                        println("逃跑失败！")
                        val enemyDamage = Random.nextInt(5, 10)
                        player.health -= enemyDamage
                        println("${enemy}对你造成了 $enemyDamage 点伤害！")
                    }
                }
            }
            if (enemyHealth <= 0) {
                val reward = when (enemy) {
                    "野狼" -> 5
                    "毒蜘蛛" -> 8
                    "强盗" -> 15
                    else -> 5
                }
                println("你击败了$enemy！获得 $reward 金币。")
                player.gold += reward

                // 随机掉落物品
                if (Random.nextBoolean()) {
                    val loot = when (enemy) {
                        "野狼" -> "狼皮"
                        "毒蜘蛛" -> "蜘蛛毒液"
                        "强盗" -> "钱袋"
                        else -> "未知物品"
                    }
                    println("${enemy}掉落了: $loot")
                    player.inventory.add(loot)
                }
            }
            if (player.health <= 0) {
                println("你被击败了！游戏结束。")
                exitProcess(0)
            }
        }
    }

    // 保存游戏
    private fun saveGame() {
        val saveData = """
            ${player.name}
            ${player.health}
            ${player.gold}
            ${player.inventory.joinToString(",")}
            ${player.activeQuests.joinToString(",")}
            ${player.completedQuests.joinToString(",")}
            ${currentLocation.name}
            """.trimIndent()
        File("SaveGame.txt").writeText(saveData)
        println("游戏已保存！")
    }

    // 加载游戏
    fun loadGame() {
        val file = File("SaveGame.txt")
        if (file.exists()) {
            val lines = file.readLines().filter { it.isNotBlank() }
            if (lines.size >= 7) {
                player.name = lines[0]
                player.health = lines[1].toIntOrNull() ?: 100
                player.gold = lines[2].toIntOrNull() ?: 10
                player.inventory = if (lines[3].isNotBlank()) {
                    lines[3].split(",").filter { it.isNotBlank() }.toMutableList()
                } else {
                    mutableListOf()
                }
                player.activeQuests = if (lines[4].isNotBlank()) {
                    lines[4].split(",").filter { it.isNotBlank() }.toMutableList()
                } else {
                    mutableListOf()
                }
                player.completedQuests = if (lines[5].isNotBlank()) {
                    lines[5].split(",").filter { it.isNotBlank() }.toMutableList()
                } else {
                    mutableListOf()
                }
                val locationName = lines[6]
                currentLocation = locations[locationName] ?: run {
                    println("错误：位置 '$locationName' 未找到，默认回到村庄")
                    locations["村庄"]!!
                }
                println("游戏已加载！")
            } else {
                println("保存文件格式错误，至少需要7行有效数据。")
            }
        } else {
            println("没有找到保存的游戏。")
        }
    }

    // 打开背包（支持多选）
    private fun openInventory() {
        if (player.inventory.isEmpty()) {
            println("你的背包是空的！")
            return
        }

        println("背包物品:")
        player.inventory.forEachIndexed { index, item ->
            println("${index + 1}. $item")
        }
        println("输入物品编号(用空格分隔多选)，或输入0返回:")

        val input = readlnOrNull()?.trim()
        when {
            input == null || input == "0" -> {
                println("返回游戏...")
                return
            }

            else -> {
                val selectedIndices = input.split(" ").asSequence().mapNotNull { it.toIntOrNull() }
                    .filter { it in 1..player.inventory.size }.map { it - 1 }.distinct().sortedDescending().toList()

                if (selectedIndices.isEmpty()) {
                    println("无效选择！")
                    return
                }

                println("你选择了: ${selectedIndices.joinToString(", ") { player.inventory[it] }}")
                println("1. 食用 | 2. 丢弃 | 0. 取消")

                when (readlnOrNull()?.toIntOrNull()) {
                    1 -> useItems(selectedIndices)
                    2 -> discardItems(selectedIndices)
                    0 -> println("取消操作。")
                    else -> println("无效选择！")
                }
            }
        }
    }

    // 批量使用物品
    private fun useItems(indices: List<Int>) {
        var healthRestored = 0
        val itemsToRemove = mutableListOf<Int>()

        indices.forEach { index ->
            when (player.inventory[index]) {
                "苹果" -> {
                    healthRestored += 20
                    itemsToRemove.add(index)
                }

                "面包" -> {
                    healthRestored += 30
                    itemsToRemove.add(index)
                }

                "草药" -> {
                    healthRestored += 40
                    itemsToRemove.add(index)
                }
            }
        }

        if (healthRestored > 0) {
            itemsToRemove.sortedDescending().forEach { index ->
                player.inventory.removeAt(index)
            }
            player.health = (player.health + healthRestored).coerceAtMost(100)
            println("你恢复了${healthRestored}点生命值！")
        } else {
            println("选中的物品中没有可食用的物品！")
        }
    }

    // 批量丢弃物品
    private fun discardItems(indices: List<Int>) {
        val discardedItems = mutableListOf<String>()
        indices.sortedDescending().forEach { index ->
            discardedItems.add(player.inventory.removeAt(index))
        }
        currentLocation.items.addAll(discardedItems)
        println("你丢弃了: ${discardedItems.joinToString(", ")}")
    }
}

fun main() {
    val game = Game()
    println("1. 新游戏 | 2. 加载游戏")
    val choice = readlnOrNull()
    if (choice == "2") {
        game.loadGame()
        game.start(isLoaded = true)
    } else {
        game.start(isLoaded = false)
    }
}