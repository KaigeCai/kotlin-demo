import java.io.File
import java.util.*
import kotlin.random.Random
import kotlin.system.exitProcess

// 玩家类
class Player {
    var name: String = "冒险者"
    var health: Int = 100
    var maxHealth: Int = 100
    var gold: Int = 10
    var inventory: MutableList<String> = mutableListOf()
    var activeQuests: MutableList<String> = mutableListOf()
    var completedQuests: MutableList<String> = mutableListOf()
    var equippedWeapon: String? = null
    var equippedArmor: String? = null
    var attackBonus: Int = 0
    var defenseBonus: Int = 0

    fun showStatus() {
        println("玩家: $name | 生命值: $health/$maxHealth | 金币: $gold")
        println("攻击力: ${5 + attackBonus} | 防御力: $defenseBonus")
        println("装备: 武器-${equippedWeapon ?: "无"} | 护甲-${equippedArmor ?: "无"}")
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
    var hasShop: Boolean = false
}

// 游戏类
class Game {
    private var player = Player()
    private var currentLocation: Location
    private val locations = mutableMapOf<String, Location>()
    private var isFirstTime = true

    private var isFishing = false
    private var fishHooked = false
    private var fishType = ""
    private var fishingAttempts = 0
    private val availableCommands = mutableListOf<String>()

    // 商店物品及价格
    private val shopItems = mapOf(
        "宝剑" to 50,
        "机枪" to 150,
        "火箭弹" to 300,
        "振金铠甲" to 200,
        "钢铁侠铠甲" to 500,
        "满血瓶" to 30
    )

    // 武器攻击力
    private val weaponStats = mapOf(
        "木剑" to 10,
        "宝剑" to 50,
        "机枪" to 88,
        "火箭弹" to 333
    )

    // 护甲防御力
    private val armorStats = mapOf(
        "振金铠甲" to 1000,
        "钢铁侠铠甲" to 888
    )

    // 更新可用命令
    private fun updateAvailableCommands() {
        availableCommands.clear()
        availableCommands.addAll(listOf("方向", "打开背包", "装备", "保存", "退出"))

        if (currentLocation.items.isNotEmpty()) {
            availableCommands.add("拾取")
        }

        if (currentLocation.hasVillager) {
            availableCommands.add("与村民对话")
        }

        if (currentLocation.hasShop) {
            availableCommands.add("商店")
        }

        if (currentLocation.name == "湖泊" && player.inventory.contains("鱼竿")) {
            availableCommands.add("钓鱼")
        }
    }

    init {
        val village = Location("村庄", "一个宁静的小村庄，村民们过着平静的生活。").apply {
            hasVillager = true
            hasShop = true
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
            hasVillager = true
        }

        // 配置多方向通向湖泊
        village.exits.apply {
            put("北", forest)
            put("东", lake)
            put("南", lake)
        }

        forest.exits.apply {
            put("南", village)
            put("东", cave)
            put("北", lake)
        }

        cave.exits.apply {
            put("西", forest)
            put("东", lake)
        }

        lake.exits.apply {
            put("西", village)
            put("东", forest)
            put("北", cave)
        }

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
            if (currentLocation.items.isNotEmpty()) {
                println("地上有: ${currentLocation.items.joinToString(", ")}")
            }
            updateAvailableCommands()
            println("输入命令(${availableCommands.joinToString("、")}): ")
            when (val input = readlnOrNull()?.lowercase(Locale.getDefault())) {
                "北", "南", "东", "西" -> move(input)
                "拾取" -> if (availableCommands.contains("拾取")) pickUpItem() else println("当前不能拾取物品")
                "与村民对话" -> if (availableCommands.contains("与村民对话")) talkToVillager() else println("这里没有村民")
                "商店" -> if (availableCommands.contains("商店")) openShop() else println("这里没有商店")
                "装备" -> manageEquipment()
                "钓鱼" -> if (availableCommands.contains("钓鱼")) startFishing() else println("这里不能钓鱼或你没有鱼竿")
                "打开背包" -> openInventory()
                "保存" -> saveGame()
                "退出" -> {
                    println("感谢游玩！")
                    break
                }

                else -> println("无效命令！")
            }

            if (Random.nextInt(10) < 4) {
                if (Random.nextBoolean() && currentLocation.name != "村庄") {
                    randomEvent()
                } else if (currentLocation.hasVillager) {
                    println("\n一位村民向你走来...")
                    talkToVillager()
                }
            }
        }
    }

    // 商店功能
    private fun openShop() {
        println("\n=== 商店 ===")
        println("你的金币: ${player.gold}")
        shopItems.forEach { (item, price) ->
            println("$item - $price 金币")
        }
        println("输入要购买的物品名称，或输入0返回:")

        val input = readlnOrNull()?.trim()
        when {
            input == null || input == "0" -> return
            shopItems.containsKey(input) -> {
                val price = shopItems[input]!!
                if (player.gold >= price) {
                    player.gold -= price
                    player.inventory.add(input)
                    println("你购买了$input！")

                    if (input == "满血瓶") {
                        player.health = player.maxHealth
                        println("你的生命值已完全恢复！")
                        player.inventory.remove("满血瓶")
                    }
                } else {
                    println("金币不足！")
                }
            }

            else -> println("商店没有这个物品！")
        }
    }

    // 装备管理
    private fun manageEquipment() {
        println("\n=== 装备管理 ===")
        println("1. 装备武器 | 2. 装备护甲 | 3. 卸下装备 | 0. 返回")

        when (readlnOrNull()?.toIntOrNull()) {
            1 -> equipWeapon()
            2 -> equipArmor()
            3 -> unEquipItems()
            0 -> return
            else -> println("无效选择！")
        }
    }

    // 装备武器
    private fun equipWeapon() {
        val weapons = player.inventory.filter { weaponStats.containsKey(it) }
        if (weapons.isEmpty()) {
            println("你没有可装备的武器！")
            return
        }

        println("选择要装备的武器:")
        weapons.forEachIndexed { index, weapon ->
            println("${index + 1}. $weapon (攻击力+${weaponStats[weapon]})")
        }
        println("0. 取消")

        when (val choice = readlnOrNull()?.toIntOrNull()) {
            null, 0 -> return
            in 1..weapons.size -> {
                val selectedWeapon = weapons[choice - 1]
                player.equippedWeapon = selectedWeapon
                player.attackBonus = weaponStats[selectedWeapon]!!
                println("你装备了$selectedWeapon！攻击力+${player.attackBonus}")
            }

            else -> println("无效选择！")
        }
    }

    // 装备护甲
    private fun equipArmor() {
        val armors = player.inventory.filter { armorStats.containsKey(it) }
        if (armors.isEmpty()) {
            println("你没有可装备的护甲！")
            return
        }

        println("选择要装备的护甲:")
        armors.forEachIndexed { index, armor ->
            println("${index + 1}. $armor (防御力+${armorStats[armor]})")
        }
        println("0. 取消")

        when (val choice = readlnOrNull()?.toIntOrNull()) {
            null, 0 -> return
            in 1..armors.size -> {
                val selectedArmor = armors[choice - 1]
                player.equippedArmor = selectedArmor
                player.defenseBonus = armorStats[selectedArmor]!!
                println("你装备了$selectedArmor！防御力+${player.defenseBonus}")
            }

            else -> println("无效选择！")
        }
    }

    // 卸下装备
    private fun unEquipItems() {
        println("1. 卸下武器 | 2. 卸下护甲 | 0. 返回")

        when (readlnOrNull()?.toIntOrNull()) {
            1 -> {
                if (player.equippedWeapon != null) {
                    println("你卸下了${player.equippedWeapon}！")
                    player.attackBonus = 0
                    player.equippedWeapon = null
                } else {
                    println("你没有装备武器！")
                }
            }

            2 -> {
                if (player.equippedArmor != null) {
                    println("你卸下了${player.equippedArmor}！")
                    player.defenseBonus = 0
                    player.equippedArmor = null
                } else {
                    println("你没有装备护甲！")
                }
            }

            0 -> return
            else -> println("无效选择！")
        }
    }

    // 移动
    private fun move(direction: String) {
        val newLocation = currentLocation.exits[direction]
        if (newLocation != null) {
            currentLocation = newLocation
            println("你移动到了${currentLocation.name}。")

            when (currentLocation.name) {
                "森林" -> {
                    println("这片茂密的森林中隐藏着危险...")
                    Thread.sleep(1000)
                    triggerEnemy("森林")
                }

                "洞穴" -> {
                    println("洞穴中传出奇怪的声音...")
                    Thread.sleep(1000)
                    triggerEnemy("洞穴")
                }
            }
        } else {
            println("你不能往那个方向走！")
        }
    }

    // 特定地点触发特定敌人
    private fun triggerEnemy(location: String) {
        val enemies = when (location) {
            "森林" -> listOf("强盗", "野狼", "毒蜘蛛")
            "洞穴" -> listOf("毒蜘蛛")
            else -> emptyList()
        }

        if (enemies.isNotEmpty()) {
            val enemy = enemies.random()
            startCombat(enemy)
        }
    }

    // 战斗逻辑
    private fun startCombat(enemy: String) {
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

            when (readlnOrNull()) {
                "1" -> {
                    enemyHealth = damage(enemyHealth, enemy)
                }

                "2" -> {
                    if (attemptEscape()) {
                        println("你成功逃跑了！")
                        return
                    } else {
                        println("逃跑失败！")
                        val enemyDamage = (calculateEnemyDamage(enemy) - player.defenseBonus).coerceAtLeast(1)
                        player.health -= enemyDamage
                        println("${enemy}对你造成了 $enemyDamage 点伤害！")
                    }
                }

                else -> println("无效输入，自动选择攻击！")
            }
        }

        if (enemyHealth <= 0) {
            val reward = calculateReward(enemy)
            println("你击败了$enemy！获得 $reward 金币。")
            player.gold += reward
        }

        if (player.health <= 0) {
            println("你被击败了！游戏结束。")
            exitProcess(0)
        }
    }

    // 计算玩家伤害
    private fun calculatePlayerDamage(): Int {
        return when {
            player.equippedWeapon != null -> weaponStats[player.equippedWeapon]!! + Random.nextInt(0, 5)
            player.inventory.contains("木剑") -> Random.nextInt(10, 20)
            else -> Random.nextInt(5, 10)
        }
    }

    // 计算敌人伤害
    private fun calculateEnemyDamage(enemy: String): Int {
        return when (enemy) {
            "野狼" -> Random.nextInt(5, 10)
            "毒蜘蛛" -> Random.nextInt(8, 13)
            "强盗" -> Random.nextInt(10, 15)
            else -> Random.nextInt(5, 10)
        }
    }

    // 计算奖励
    private fun calculateReward(enemy: String): Int {
        return when (enemy) {
            "野狼" -> 5
            "毒蜘蛛" -> 8
            "强盗" -> 15
            else -> 5
        }
    }

    // 尝试逃跑
    private fun attemptEscape(): Boolean {
        return Random.nextFloat() < 0.5f
    }

    // 拾取物品
    private fun pickUpItem() {
        if (currentLocation.items.isNotEmpty()) {
            val item = currentLocation.items[0]
            if (item == "鱼竿" && player.activeQuests.contains("钓鱼")) {
                player.inventory.add(item)
                currentLocation.items.removeAt(0)
                println("你拾取了鱼竿。输入'钓鱼'开始钓鱼操作。")
            } else {
                val currentItem = currentLocation.items.removeAt(0)
                player.inventory.add(currentItem)
                println("你拾取了: $currentItem")
            }
        } else {
            println("这里没有可拾取的物品。")
        }
    }

    // 与村民对话
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
                    when {
                        !player.completedQuests.contains("钓鱼") && !player.activeQuests.contains("钓鱼") -> {
                            println("渔夫说：你好啊，${player.name}！能帮我钓几条鱼吗？我可以借你鱼竿。")
                            println("1. 接受任务 | 2. 拒绝")
                            val choice = readlnOrNull()
                            if (choice == "1") {
                                player.activeQuests.add("钓鱼")
                                currentLocation.items.add("鱼竿")
                                println("你接受了任务：钓鱼")
                                println("渔夫说：太好了！我把鱼竿放在地上了，拾取它就可以开始钓鱼。")
                            }
                        }

                        player.activeQuests.contains("钓鱼") && player.inventory.contains("鱼") -> {
                            println("渔夫说：哇，你钓到鱼了！太棒了！这是你的奖励。")
                            player.inventory.remove("鱼")
                            player.gold += 20
                            player.activeQuests.remove("钓鱼")
                            player.completedQuests.add("钓鱼")
                        }

                        player.activeQuests.contains("钓鱼") -> {
                            println("渔夫说：用鱼竿在湖边钓鱼就能抓到鱼。")
                        }
                    }
                }
            }
        } else {
            println("这里没有村民可以对话。")
        }
    }

    // 开始钓鱼
    private fun startFishing() {
        if (currentLocation.name != "湖泊") {
            println("这里不是适合钓鱼的地方！")
            return
        }

        if (!player.inventory.contains("鱼竿")) {
            println("你需要鱼竿才能钓鱼！")
            return
        }

        isFishing = true
        fishingAttempts++
        println("你甩出了鱼线...等待鱼儿上钩...")

        Thread.sleep(2000)

        // 60%食人鱼 30%鱼 10%脱钩
        when (Random.nextInt(100)) {
            in 0..59 -> {
                fishType = "食人鱼"
                println("哇！一条凶猛的食人鱼上钩了！")
            }

            in 60..89 -> {
                fishType = "鱼"
                println("有鱼上钩了！")
            }

            else -> {
                println("鱼儿脱钩了...")
                isFishing = false
                return
            }
        }

        fishHooked = true
        handleFishFight()
    }

    private fun handleFishFight() {
        var fishStamina = Random.nextInt(3, 6)
        var playerStamina = 5

        while (fishHooked && fishStamina > 0 && playerStamina > 0) {
            println("\n鱼在猛烈挣扎！(鱼体力: $fishStamina, 你的体力: $playerStamina)")
            println("快速按下1收线！")

            val input = readlnOrNull()
            if (input == "1") {
                fishStamina--
                println("你成功收线！")
            } else {
                playerStamina--
                println("反应太慢！你失去了平衡。")
            }

            if (fishType == "食人鱼" && Random.nextBoolean()) {
                val damage = Random.nextInt(5, 10)
                player.health -= damage
                println("食人鱼咬了你！失去${damage}点生命值。")
            }
        }

        if (fishStamina <= 0) {
            println("成功钓到了一条$fishType！")
            player.inventory.add(fishType)
            if (fishType == "食人鱼") {
                println("这只食人鱼还在挣扎！小心处理！")
            }
        } else {
            println("鱼儿逃脱了...")
        }

        isFishing = false
        fishHooked = false

        if (fishingAttempts > 0 && fishingAttempts % 3 == 0) {
            if (Random.nextBoolean()) {
                player.inventory.remove("鱼竿")
                println("你的鱼竿损坏了！")
            }
        }
    }

    // 随机事件
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
                    enemyHealth = damage(enemyHealth, enemy)
                } else {
                    if (attemptEscape()) {
                        println("你成功逃跑了！")
                        break
                    } else {
                        println("逃跑失败！")
                        val enemyDamage = (calculateEnemyDamage(enemy) - player.defenseBonus).coerceAtLeast(1)
                        player.health -= enemyDamage
                        println("${enemy}对你造成了 $enemyDamage 点伤害！")
                    }
                }
            }
            if (enemyHealth <= 0) {
                val reward = calculateReward(enemy)
                println("你击败了$enemy！获得 $reward 金币。")
                player.gold += reward

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

    private fun damage(enemyHealth: Int, enemy: String): Int {
        var enemyHealth1 = enemyHealth
        val damage = calculatePlayerDamage()
        enemyHealth1 -= damage
        println("你对${enemy}造成了 $damage 点伤害！")
        if (enemyHealth1 > 0) {
            val enemyDamage = (calculateEnemyDamage(enemy) - player.defenseBonus).coerceAtLeast(1)
            player.health -= enemyDamage
            println("${enemy}对你造成了 $enemyDamage 点伤害！")
        }
        return enemyHealth1
    }

    // 保存游戏
    private fun saveGame() {
        val saveData = """
            ${player.name}
            ${player.health}
            ${player.maxHealth}
            ${player.gold}
            ${player.inventory.joinToString(",")}
            ${player.activeQuests.joinToString(",")}
            ${player.completedQuests.joinToString(",")}
            ${currentLocation.name}
            ${player.equippedWeapon ?: "null"}
            ${player.equippedArmor ?: "null"}
            """.trimIndent()
        File("SaveGame.txt").writeText(saveData)
        println("游戏已保存！")
    }

    // 加载游戏
    fun loadGame() {
        val file = File("SaveGame.txt")
        if (file.exists()) {
            val lines = file.readLines().filter { it.isNotBlank() }
            if (lines.size >= 10) {
                player.name = lines[0]
                player.health = lines[1].toIntOrNull() ?: 100
                player.maxHealth = lines[2].toIntOrNull() ?: 100
                player.gold = lines[3].toIntOrNull() ?: 10
                player.inventory = if (lines[4].isNotBlank()) {
                    lines[4].split(",").filter { it.isNotBlank() }.toMutableList()
                } else {
                    mutableListOf()
                }
                player.activeQuests = if (lines[5].isNotBlank()) {
                    lines[5].split(",").filter { it.isNotBlank() }.toMutableList()
                } else {
                    mutableListOf()
                }
                player.completedQuests = if (lines[6].isNotBlank()) {
                    lines[6].split(",").filter { it.isNotBlank() }.toMutableList()
                } else {
                    mutableListOf()
                }
                val locationName = lines[7]
                currentLocation = locations[locationName] ?: run {
                    println("错误：位置 '$locationName' 未找到，默认回到村庄")
                    locations["村庄"]!!
                }

                player.equippedWeapon = if (lines[8] != "null") lines[8] else null
                player.equippedArmor = if (lines[9] != "null") lines[9] else null
                player.attackBonus = player.equippedWeapon?.let { weaponStats[it] ?: 0 } ?: 0
                player.defenseBonus = player.equippedArmor?.let { armorStats[it] ?: 0 } ?: 0

                println("游戏已加载！")
            } else {
                println("保存文件格式错误，至少需要10行有效数据。")
            }
        } else {
            println("没有找到保存的游戏。")
        }
    }

    // 打开背包
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
        var curedPoison = false

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
                    curedPoison = true
                    itemsToRemove.add(index)
                }
            }
        }

        if (healthRestored > 0) {
            itemsToRemove.sortedDescending().forEach { index ->
                player.inventory.removeAt(index)
            }
            player.health = (player.health + healthRestored).coerceAtMost(player.maxHealth)
            println("你恢复了${healthRestored}点生命值！")
            if (curedPoison) {
                println("草药治愈了你的身体！")
            }
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