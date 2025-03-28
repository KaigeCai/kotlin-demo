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

    fun showStatus() {
        println("玩家: $name | 生命值: $health | 金币: $gold")
        println("背包: ${inventory.joinToString(", ")}")
    }
}

// 地点类
class Location(val name: String, val description: String) {
    val exits: MutableMap<String, Location> = mutableMapOf()
    val items: MutableList<String> = mutableListOf()
}

// 游戏类
class Game {
    private var player = Player()
    private var currentLocation: Location
    private val locations = mutableMapOf<String, Location>()

    init {
        val village = Location("村庄", "一个宁静的小村庄，村民们过着平静的生活。")
        val forest = Location("森林", "一片茂密的森林，隐藏着未知的危险。")
        val cave = Location("洞穴", "一个阴暗的洞穴，传出奇怪的声音。")

        village.exits["北"] = forest
        forest.exits["南"] = village
        forest.exits["东"] = cave
        cave.exits["西"] = forest

        village.items.add("苹果")
        forest.items.add("木剑")

        locations["村庄"] = village
        locations["森林"] = forest
        locations["洞穴"] = cave

        currentLocation = village
    }

    // 游戏主循环
    fun start(isLoaded: Boolean = false) {
        if (!isLoaded) {
            println("欢迎来到冒险游戏！请输入你的名字：")
            player.name = readlnOrNull()?.takeIf { it.isNotBlank() } ?: "冒险者"
        }
        println("你醒来发现自己在${currentLocation.name}。冒险开始了！")

        while (true) {
            println()
            player.showStatus()
            println("你当前在: ${currentLocation.name}")
            println(currentLocation.description)
            println("可去的方向: ${currentLocation.exits.keys.joinToString(", ")}")
            println("地上有: ${currentLocation.items.joinToString(", ")}")
            println("输入命令（北、南、东、西、拾取、与村民对话、打开背包、保存、退出）：")
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

            // 随机事件
            if (Random.nextInt(10) < 3) {
                randomEvent()
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

    // 与村民对话
    private fun talkToVillager() {
        if (currentLocation.name == "村庄") {
            println("村民说：欢迎你，${player.name}！我需要一柄木剑，你能帮我找来吗？")
            println("1. 接受任务 | 2. 拒绝")
            val choice = readlnOrNull()
            if (choice == "1") {
                if (player.inventory.remove("木剑")) {
                    println("村民说：谢谢你！这是你的奖励。")
                    player.gold += 10
                } else {
                    println("村民说：请先找到木剑再回来吧。")
                }
            } else {
                println("村民说：好吧，也许下次吧。")
            }
        } else {
            println("这里没有村民可以对话。")
        }
    }

    // 随机事件（战斗）
    private fun randomEvent() {
        if (currentLocation.name != "村庄") {
            println("一只野狼突然跳了出来！")
            var enemyHealth = 20
            while (enemyHealth > 0 && player.health > 0) {
                println("野狼生命值: $enemyHealth | 你的生命值: ${player.health}")
                println("1. 攻击 | 2. 逃跑")
                val choice = readlnOrNull()
                if (choice == "1") {
                    val damage = Random.nextInt(5, 15)
                    enemyHealth -= damage
                    println("你对野狼造成了 $damage 点伤害！")
                    if (enemyHealth > 0) {
                        val enemyDamage = Random.nextInt(5, 10)
                        player.health -= enemyDamage
                        println("野狼对你造成了 $enemyDamage 点伤害！")
                    }
                } else {
                    println("你成功逃跑了！")
                    break
                }
            }
            if (enemyHealth <= 0) {
                println("你击败了野狼！获得 5 金币。")
                player.gold += 5
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
            if (lines.size >= 4) {
                player.name = lines[0]
                player.health = lines[1].toIntOrNull() ?: 100
                player.gold = lines[2].toIntOrNull() ?: 10
                player.inventory = if (lines[3].isNotBlank() && lines[3] != "村庄") {
                    lines[3].split(",").filter { it.isNotBlank() }.toMutableList()
                } else {
                    mutableListOf()
                }
                val locationName = if (lines.size > 4) lines[4] else lines.getOrElse(3) { "村庄" }
                currentLocation = locations[locationName] ?: run {
                    println("错误：位置 '$locationName' 未找到，默认回到村庄")
                    locations["村庄"]!!
                }
                println("游戏已加载！")
            } else {
                println("保存文件格式错误，至少需要4行有效数据。")
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
                    .filter { it in 1..player.inventory.size }.map { it - 1 }.distinct().sortedDescending()
                    .toList() // 从大到小排序便于删除操作

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
            }
        }

        if (healthRestored > 0) {
            // 从大到小删除避免索引错位
            itemsToRemove.sortedDescending().forEach { index ->
                player.inventory.removeAt(index)
            }
            player.health = (player.health + healthRestored).coerceAtMost(100)
            println("你食用了${itemsToRemove.size}个苹果，恢复了${healthRestored}点生命值！")
        } else {
            println("选中的物品中没有可食用的物品！")
        }
    }

    // 批量丢弃物品
    private fun discardItems(indices: List<Int>) {
        val discardedItems = mutableListOf<String>()
        // 从大到小删除避免索引错位
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