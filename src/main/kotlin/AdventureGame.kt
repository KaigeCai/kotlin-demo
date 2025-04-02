import java.io.File
import java.util.*
import kotlin.random.Random
import kotlin.system.exitProcess

// 添加任务常量
private const val banditQuestName = "剿灭强盗"
private const val banditQuestRequirement = 5 // 需要击败的强盗数量
private var banditsDefeated = 0 // 记录已击败强盗数量
private var shouldWarnAboutBoss = false

// 玩家类
class Player {
    var name: String = "冒险者"
    var health: Int = 100
    private var baseMaxHealth: Int = 100
    var gold: Int = 10000
    var inventory: MutableList<String> = mutableListOf()
    var activeQuests: MutableList<String> = mutableListOf()
    var completedQuests: MutableList<String> = mutableListOf()
    var equippedWeapon: String? = null
    var equippedArmor: String? = null
    var attackBonus: Int = 0
    var defenseBonus: Int = 0
    var maxHealth: Int = baseMaxHealth + defenseBonus

    var lastRestTime: Long = System.currentTimeMillis()
    val restCooldown: Long = 60000 // 1分钟冷却时间
    val restHealAmount: Int = 30 // 每次休息恢复的生命值

    fun showStatus() {
        println("\n玩家: $name | 生命值: $health/$maxHealth | 金币: $gold")
        println("攻击力: ${5 + attackBonus} | 防御力: $defenseBonus")
        println("装备: 武器-${equippedWeapon ?: "无"} | 护甲-${equippedArmor ?: "无"}")

        // 显示剿灭强盗任务进度
        if (activeQuests.contains(banditQuestName)) {
            println("剿灭强盗进度: $banditQuestName ($banditsDefeated/$banditQuestRequirement)")
        }

        // 添加Boss战提示
        if (shouldWarnAboutBoss) {
            println("警告: 所有任务已完成，洞穴深处似乎有强大的存在苏醒了...")
        }

        // 修改背包显示逻辑
        if (inventory.isEmpty()) {
            println("背包: 空")
        } else {
            val groupedItems = inventory.groupingBy { it }.eachCount()
            val inventoryDisplay = groupedItems.entries.joinToString(", ") {
                if (it.value > 1) "${it.key}x${it.value}" else it.key
            }
            println("背包: $inventoryDisplay")
        }

        if (activeQuests.isNotEmpty()) {
            println("进行中的任务: ${activeQuests.distinct().joinToString(", ")}")
        }
        if (completedQuests.isNotEmpty()) {
            println("已完成的任务: ${completedQuests.distinct().joinToString(", ")}")
        }
    }

    fun addActiveQuest(questName: String) {
        if (!activeQuests.contains(questName)) {
            activeQuests.add(questName)
        }
    }

    fun addCompletedQuest(questName: String) {
        if (!completedQuests.contains(questName)) {
            completedQuests.add(questName)
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
    private var finalBossHealth = FINAL_BOSS_MAX_HEALTH
    private var finalBossPhase = 1
    private var finalBossDefeated = false
    private var princessRescued = false
    private var finalBattleTriggered = false

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
        "宝剑" to 50, "机枪" to 150, "火箭弹" to 300, "振金铠甲" to 200, "钢铁侠铠甲" to 500, "满血瓶" to 100
    )

    // 武器攻击力
    private val weaponStats = mapOf(
        "木剑" to 10, "宝剑" to 50, "机枪" to 88, "火箭弹" to 333, "短剑" to 30, "手枪" to 100
    )

    // 护甲防御力
    private val armorStats = mapOf(
        "振金铠甲" to 1000, "钢铁侠铠甲" to 888, "皮甲" to 200
    )

    // 物品售价
    private val itemPrices = mapOf(
        "钱袋" to 1000,
        "食人鱼" to 800,
        "狼皮" to 300,
        "蜘蛛毒液" to 500,
        "矿石" to 150,
        "苹果" to 10,
        "面包" to 15,
        "草药" to 25,
        "火把" to 30,
        "蝙蝠翅膀" to 400,
        "蝙蝠牙齿" to 600,
        "短剑" to 350,
        "皮甲" to 500,
        "强盗徽章" to 200,
        "烈酒" to 80,
        "手枪" to 300
    )

    // 更新可用命令
    private fun updateAvailableCommands() {
        availableCommands.clear()
        availableCommands.addAll(listOf("方向", "背包", "商店", "装备", "保存", "退出", "村庄"))

        if (currentLocation.items.isNotEmpty()) {
            availableCommands.add("拾取")
        }

        if (currentLocation.hasVillager) {
            availableCommands.add("对话")
        }

        if (currentLocation.name == "湖泊" && player.inventory.contains("鱼竿")) {
            availableCommands.add("钓鱼")
        }
    }

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
            shouldWarnAboutBoss = checkAllQuestsCompleted() && !finalBossDefeated
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
                "北", "南", "西", "东" -> move(input)
                "拾取" -> if (availableCommands.contains("拾取")) pickUpItem() else println("当前不能拾取物品")
                "对话" -> if (availableCommands.contains("对话")) talkToVillager() else println("这里没有村民")
                "商店" -> openShop()
                "装备" -> manageEquipment()
                "钓鱼" -> if (availableCommands.contains("钓鱼")) startFishing() else println("这里不能钓鱼或你没有鱼竿")
                "背包" -> openInventory()
                "保存" -> saveGame()
                "村庄" -> returnToVillage()
                "退出" -> {
                    println("感谢游玩！")
                    break
                }

                else -> println("无效命令！")
            }

            if (Random.nextInt(10) < 4) {
                if (Random.nextBoolean() && currentLocation.name != "村庄") {
                    triggerCombat()
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
        println("1. 购买 | 2. 出售 | 0. 返回")

        when (readlnOrNull()?.toIntOrNull()) {
            1 -> showBuyMenu()
            2 -> showSellMenu()
            0 -> return
            else -> println("无效选择！")
        }
    }

    // 购买菜单
    private fun showBuyMenu() {
        println("\n=== 购买 ===")
        println("你的金币: ${player.gold}")
        shopItems.forEach { (item, price) ->
            println("$item - $price 金币")
        }
        println("输入要购买的物品名称(用空格分隔多个物品)，或输入0返回:")

        val input = readlnOrNull()?.trim()
        when {
            input == null || input == "0" -> return
            else -> {
                val itemsToBuy = input.split(" ").filter { it.isNotBlank() }
                if (itemsToBuy.isEmpty()) {
                    println("没有输入有效物品！")
                    return
                }

                // 检查所有物品是否在商店中
                val invalidItems = itemsToBuy.filterNot { shopItems.containsKey(it) }
                if (invalidItems.isNotEmpty()) {
                    println("以下物品不在商店中: ${invalidItems.joinToString(", ")}")
                    return
                }

                // 计算总价
                val totalCost = itemsToBuy.sumOf { shopItems[it] ?: 0 }
                if (player.gold < totalCost) {
                    println("金币不足！需要 $totalCost 金币，但你只有 ${player.gold} 金币。")
                    return
                }

                // 确认购买
                println("确定花费 $totalCost 金币购买 ${itemsToBuy.joinToString(", ")} 吗？(1. 是 | 2. 否)")
                when (readlnOrNull()?.toIntOrNull()) {
                    1 -> {
                        player.gold -= totalCost
                        itemsToBuy.forEach { item ->
                            player.inventory.add(item)
                            // 处理装备购买后自动装备的情况
                            when {
                                weaponStats.containsKey(item) -> {
                                    player.equippedWeapon = item
                                    player.attackBonus = weaponStats[item]!!
                                    println("你自动装备了$item！攻击力+${player.attackBonus}")
                                }

                                armorStats.containsKey(item) -> {
                                    player.equippedArmor = item
                                    player.defenseBonus = armorStats[item]!!
                                    player.maxHealth = 100 + player.defenseBonus
                                    println("你自动装备了$item！防御力+${player.defenseBonus}")
                                    println("你的最大生命值现在是${player.maxHealth}")
                                }

                                item == "满血瓶" -> {
                                    player.health = player.maxHealth
                                    println("你的生命值已完全恢复！")
                                    player.inventory.remove("满血瓶")
                                }
                            }
                        }
                        println("购买成功！获得: ${itemsToBuy.joinToString(", ")}")
                    }

                    else -> println("取消购买。")
                }
            }
        }
    }

    // 出售菜单
    private fun showSellMenu() {
        if (player.inventory.isEmpty()) {
            println("你的背包是空的，没有物品可以出售！")
            return
        }

        println("\n=== 出售 ===")
        println("你的金币: ${player.gold}")
        println("背包物品:")

        val sellableItems = player.inventory.filter { itemPrices.containsKey(it) }
        if (sellableItems.isEmpty()) {
            println("没有可以出售的物品！")
            return
        }

        val groupedItems = sellableItems.groupingBy { it }.eachCount()
        groupedItems.entries.forEachIndexed { index, entry ->
            val price = itemPrices[entry.key] ?: 0
            println("${index + 1}. ${entry.key}x${entry.value} - 单价:$price 金币")
        }
        println("输入要出售的物品编号和数量(如:1 2表示出售第一个物品2个)，或输入0返回:")

        val input = readlnOrNull()?.trim()?.split(" ")
        when {
            input == null || input.size !in 1..2 || input[0] == "0" -> return
            else -> {
                val itemIndex = input[0].toIntOrNull()?.minus(1)
                val itemCount = input.getOrNull(1)?.toIntOrNull() ?: 1

                if (itemIndex == null || itemIndex !in groupedItems.keys.indices) {
                    println("无效的物品编号！")
                    return
                }

                val selectedItem = groupedItems.keys.elementAt(itemIndex)
                val availableCount = groupedItems[selectedItem] ?: 0

                if (itemCount <= 0 || itemCount > availableCount) {
                    println("无效的数量！")
                    return
                }

                val price = itemPrices[selectedItem] ?: 0
                val totalPrice = price * itemCount

                // 确认出售
                println("确定以${totalPrice}金币出售${selectedItem}x${itemCount}吗？(1. 是 | 2. 否)")
                when (readlnOrNull()?.toIntOrNull()) {
                    1 -> {
                        repeat(itemCount) {
                            player.inventory.remove(selectedItem)
                        }
                        player.gold += totalPrice
                        println("出售成功！获得${totalPrice}金币。")
                    }

                    else -> println("取消出售。")
                }
            }
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
                player.maxHealth = 100 + player.defenseBonus
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
                    triggerCombat()
                }

                "洞穴" -> {
                    println("洞穴中传出奇怪的声音...")
                    Thread.sleep(1000)
                    triggerCombat()
                    // 检查是否触发Boss战
                    if (checkAllQuestsCompleted() && !finalBossDefeated) {
                        Thread.sleep(1500)
                        println("\n突然，洞穴深处传来一阵恐怖的咆哮！")
                        println("整个洞穴开始震动，岩石从顶部掉落！")
                        Thread.sleep(2000)
                        println("\n一个巨大的黑影从洞穴深处浮现...")
                        println("$FINAL_BOSS_NAME 出现了！")
                        finalBattleTriggered = true
                        triggerFinalBossBattle()
                    }
                }

                "村庄" -> {
                    checkRestInVillage()
                }
            }
        } else {
            println("你不能往那个方向走！")
        }
    }

    // 触发敌人并战斗
    private fun triggerCombat() {
        val enemy = when (currentLocation.name) {
            "森林" -> listOf("强盗", "野狼").random()
            "洞穴" -> listOf("毒蜘蛛", "蝙蝠").random()
            else -> null
        }
        enemy?.let {
            println("一只${enemy}突然跳了出来！")
            var enemyHealth = when (enemy) {
                "野狼" -> 20
                "毒蜘蛛" -> 15
                "强盗" -> 300
                "蝙蝠" -> 80
                else -> 0
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

                // 如果是强盗且任务进行中，增加计数
                if (enemy == "强盗" && player.activeQuests.contains(banditQuestName)) {
                    banditsDefeated++
                    println("(任务进度: $banditsDefeated/$banditQuestRequirement)")
                }

                // 随机掉落物品
                val loot = when (enemy) {
                    "野狼" -> listOf("狼皮")
                    "毒蜘蛛" -> listOf("蜘蛛毒液")
                    "强盗" -> listOf("钱袋", "手枪", "短剑", "皮甲", "强盗徽章", "烈酒")
                    "蝙蝠" -> listOf("蝙蝠翅膀", "蝙蝠牙齿")
                    else -> emptyList()
                }
                if (loot.isNotEmpty()) {
                    println("${enemy}掉落了: $loot")
                    player.inventory.addAll(loot)
                }
            }

            if (player.health <= 0) {
                println("你被击败了！游戏结束。")
                exitProcess(0)
            }
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
            "蝙蝠" -> Random.nextInt(20, 50)
            else -> Random.nextInt(5, 10)
        }
    }

    // 计算奖励
    private fun calculateReward(enemy: String): Int {
        return when (enemy) {
            "野狼" -> 5
            "毒蜘蛛" -> 8
            "强盗" -> 15
            "蝙蝠" -> 12
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
            // 检查是否有鱼竿任务相关物品
            val fishingRodIndex = currentLocation.items.indexOfFirst { it == "鱼竿" }
            if (fishingRodIndex != -1 && player.activeQuests.contains("钓鱼")) {
                val item = currentLocation.items.removeAt(fishingRodIndex)
                player.inventory.add(item)
                println("你拾取了鱼竿。输入'钓鱼'开始钓鱼操作。")

                // 继续拾取其他物品
                if (currentLocation.items.isNotEmpty()) {
                    val remainingItems = currentLocation.items.toList()
                    player.inventory.addAll(remainingItems)
                    currentLocation.items.clear()
                    println("你还拾取了: ${remainingItems.joinToString(", ")}")
                }
            } else {
                // 一次性拾取所有物品
                val pickedItems = currentLocation.items.toList()
                player.inventory.addAll(pickedItems)
                currentLocation.items.clear()
                println("你拾取了: ${pickedItems.joinToString(", ")}")
            }
        } else {
            println("这里没有可拾取的物品。")
        }
    }

    // 对话
    private fun talkToVillager() {
        if (!currentLocation.hasVillager) {
            println("这里没有村民可以对话。")
            return
        }

        when (currentLocation.name) {
            "村庄" -> handleVillageDialogue()
            "湖泊" -> handleLakeDialogue()
        }
    }

    private fun handleVillageDialogue() {
        when {
            !player.completedQuests.contains(banditQuestName) && !player.activeQuests.contains(banditQuestName) -> {
                println("\n镇长忧心忡忡地说：最近强盗猖獗，你能帮忙剿灭${banditQuestRequirement}个强盗吗？")
                println("1. 接受任务 | 2. 拒绝")

                when (readlnOrNull()) {
                    "1" -> {
                        player.addActiveQuest(banditQuestName)
                        banditsDefeated = 0
                        println("你接受了任务：$banditQuestName")
                    }

                    else -> println("镇长叹了口气：希望有人能解决这个问题...")
                }
            }

            player.activeQuests.contains(banditQuestName) -> {
                if (banditsDefeated >= banditQuestRequirement) {
                    val rewardGold = 3000
                    println("\n镇长激动地说：你剿灭了${banditsDefeated}个强盗！这是你的奖励。")
                    println("获得 $rewardGold 金币！")
                    player.gold += rewardGold
                    player.activeQuests.remove(banditQuestName)
                    player.addCompletedQuest(banditQuestName)
                    banditsDefeated = 0
                } else {
                    println("\n镇长：还需要剿灭${banditQuestRequirement - banditsDefeated}个强盗！")
                }
            }
        }

        when {
            player.activeQuests.contains("寻找木剑") && player.inventory.contains("木剑") -> {
                println("村民说：你找到了木剑！太感谢了！这是你的奖励。")
                player.inventory.remove("木剑")
                player.gold += 15
                player.activeQuests.remove("寻找木剑")
                player.addCompletedQuest("寻找木剑")
            }

            !player.completedQuests.contains("寻找木剑") && !player.activeQuests.contains("寻找木剑") -> {
                println("村民说：欢迎你，${player.name}！我需要一柄木剑，你能帮我从森林里找来吗？")
                println("1. 接受任务 | 2. 拒绝")
                when (readlnOrNull()) {
                    "1" -> {
                        player.addActiveQuest("寻找木剑")
                        println("你接受了任务：寻找木剑")
                    }

                    else -> println("村民说：好吧，也许下次吧。")
                }
            }

            player.activeQuests.contains("寻找木剑") -> {
                println("村民说：你找到木剑了吗？它在森林里。")
            }
        }

        when {
            player.completedQuests.contains("寻找木剑") && !player.completedQuests.contains("收集矿石") && !player.activeQuests.contains(
                "收集矿石"
            ) -> {
                println("\n铁匠说：我看到你帮助了村民，能帮我从洞穴收集一些矿石吗？")
                println("1. 接受任务 | 2. 拒绝")
                when (readlnOrNull()) {
                    "1" -> {
                        player.addActiveQuest("收集矿石")
                        println("你接受了任务：收集矿石")
                    }

                    else -> println("铁匠说：好吧，也许下次吧。")
                }
            }

            player.activeQuests.contains("收集矿石") && player.inventory.contains("矿石") -> {
                println("\n铁匠说：太好了，你带来了矿石！这是你的奖励。")
                player.inventory.remove("矿石")
                player.gold += 25
                player.activeQuests.remove("收集矿石")
                player.addCompletedQuest("收集矿石")
            }

            player.activeQuests.contains("收集矿石") -> {
                println("\n铁匠说：你找到矿石了吗？它们在洞穴里。")
            }
        }
    }

    private fun handleLakeDialogue() {
        when {
            !player.completedQuests.contains("钓鱼") && !player.activeQuests.contains("钓鱼") -> {
                println("渔夫说：你好啊，${player.name}！能帮我钓几条鱼吗？我可以借你鱼竿。")
                println("1. 接受任务 | 2. 拒绝")
                when (readlnOrNull()) {
                    "1" -> {
                        player.addActiveQuest("钓鱼")
                        currentLocation.items.add("鱼竿")
                        println("你接受了任务：钓鱼")
                        println("渔夫说：太好了！我把鱼竿放在地上了，拾取它就可以开始钓鱼。")
                    }

                    else -> Unit
                }
            }

            player.activeQuests.contains("钓鱼") && player.inventory.contains("鱼") -> {
                println("渔夫说：哇，你钓到鱼了！太棒了！这是你的奖励。")
                player.inventory.remove("鱼")
                player.gold += 20
                player.activeQuests.remove("钓鱼")
                player.addCompletedQuest("钓鱼")
            }

            player.activeQuests.contains("钓鱼") -> {
                println("渔夫说：用鱼竿在湖边钓鱼就能抓到鱼。")
            }

            player.completedQuests.contains("钓鱼") -> {
                println("渔夫说：想再帮我钓鱼吗？")
                println("1. 接受任务 | 2. 拒绝")
                when (readlnOrNull()) {
                    "1" -> {
                        player.addActiveQuest("钓鱼")
                        if (!player.inventory.contains("鱼竿")) {
                            currentLocation.items.add("鱼竿")
                            println("渔夫说：我把鱼竿放在地上了。")
                        }
                    }

                    else -> Unit
                }
            }
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

    private fun damage(enemyHealth: Int, enemy: String): Int {
        var enemyHP = enemyHealth
        val damage = calculatePlayerDamage()
        enemyHP -= damage
        println("你对${enemy}造成了 $damage 点伤害！")
        if (enemyHP > 0) {
            val enemyDamage = (calculateEnemyDamage(enemy) - player.defenseBonus).coerceAtLeast(1)
            player.health -= enemyDamage
            println("${enemy}对你造成了 $enemyDamage 点伤害！")
        }
        return enemyHP
    }

    // 保存游戏
    private fun saveGame() {
        try {
            val saveData = buildString {
                appendLine("玩家姓名:${player.name}")
                appendLine("生命值:${player.health}")
                appendLine("最大生命值:${player.maxHealth}")
                appendLine("金币:${player.gold}")
                appendLine("背包:${player.inventory.joinToString(",")}")
                appendLine("进行中的任务:${player.activeQuests.distinct().joinToString(",")}")
                appendLine("已完成的任务:${player.completedQuests.distinct().joinToString(",")}")
                appendLine("当前位置:${currentLocation.name}")
                appendLine("装备武器:${player.equippedWeapon ?: "无"}")
                appendLine("装备护甲:${player.equippedArmor ?: "无"}")
                appendLine("剿灭强盗进度:$banditsDefeated")
            }
            File("SaveGame.txt").writeText(saveData)
            println("游戏已成功保存！")
        } catch (e: Exception) {
            println("保存游戏时出错: ${e.message}")
        }
    }

    // 加载游戏
    fun loadGame() {
        val file = File("SaveGame.txt")
        if (file.exists()) {
            try {
                val saveData = mutableMapOf<String, String>()
                file.readLines().filter { it.isNotBlank() }.forEach { line ->
                    val parts = line.split(":", limit = 2)
                    if (parts.size == 2) {
                        saveData[parts[0]] = parts[1]
                    }
                }

                player.name = saveData["玩家姓名"] ?: "冒险者"
                player.health = saveData["生命值"]?.toIntOrNull() ?: 100
                player.maxHealth = saveData["最大生命值"]?.toIntOrNull() ?: 100
                player.gold = saveData["金币"]?.toIntOrNull() ?: 10

                player.inventory =
                    saveData["背包"]?.split(",")?.filter { it.isNotBlank() }?.toMutableList() ?: mutableListOf()
                player.activeQuests =
                    saveData["进行中的任务"]?.split(",")?.filter { it.isNotBlank() }?.toMutableList() ?: mutableListOf()
                player.completedQuests =
                    saveData["已完成的任务"]?.split(",")?.filter { it.isNotBlank() }?.toMutableList() ?: mutableListOf()

                val locationName = saveData["当前位置"] ?: "村庄"
                currentLocation = locations[locationName] ?: locations["村庄"]!!

                player.equippedWeapon = saveData["装备武器"]?.takeIf { it != "无" }
                player.equippedArmor = saveData["装备护甲"]?.takeIf { it != "无" }

                player.attackBonus = player.equippedWeapon?.let { weaponStats[it] ?: 0 } ?: 0
                player.defenseBonus = player.equippedArmor?.let { armorStats[it] ?: 0 } ?: 0
                banditsDefeated = saveData["剿灭强盗进度"]?.toIntOrNull() ?: 0

                println("游戏已加载！")
            } catch (e: Exception) {
                println("加载游戏时出错: ${e.message}")
            }
        } else {
            println("没有找到保存的游戏。")
        }
    }

    // 背包
    private fun openInventory() {
        if (player.inventory.isEmpty()) {
            println("你的背包是空的！")
            return
        }

        println("背包物品:")
        val groupedItems = player.inventory.groupingBy { it }.eachCount()
        groupedItems.entries.forEachIndexed { index, entry ->
            println("${index + 1}. ${entry.key}${if (entry.value > 1) "x${entry.value}" else ""}")
        }

        println("输入物品编号(用空格分隔多选)，或输入0返回:")

        val input = readlnOrNull()?.trim()
        when {
            input == null || input == "0" -> {
                println("返回游戏...")
                return
            }

            else -> {
                val itemList = groupedItems.keys.toList()
                val selectedIndices =
                    input.split(" ").asSequence().mapNotNull { it.toIntOrNull() }.filter { it in 1..itemList.size }
                        .map { it - 1 }.distinct().sortedDescending().toList()

                if (selectedIndices.isEmpty()) {
                    println("无效选择！")
                    return
                }

                val selectedItems = selectedIndices.map { itemList[it] }
                println("你选择了: ${selectedItems.joinToString(", ")}")
                println("1. 食用 | 2. 装备 | 3. 丢弃 | 0. 取消")

                when (readlnOrNull()?.toIntOrNull()) {
                    1 -> edibleItems(selectedItems)
                    2 -> equipItems(selectedItems)
                    3 -> discardItems(selectedItems)
                    0 -> println("取消操作。")
                    else -> println("无效选择！")
                }
            }
        }
    }

    // 批量装备物品
    private fun equipItems(selectedItems: List<String>) {
        selectedItems.forEach { item ->
            when {
                weaponStats.containsKey(item) -> {
                    player.equippedWeapon = item
                    player.attackBonus = weaponStats[item]!!
                    println("你装备了$item！攻击力+${player.attackBonus}")
                }

                armorStats.containsKey(item) -> {
                    player.equippedArmor = item
                    player.defenseBonus = armorStats[item]!!
                    player.maxHealth = 100 + player.defenseBonus
                    println("你装备了$item！防御力+${player.defenseBonus}")
                    println("你的最大生命值现在是${player.maxHealth}")
                }

                else -> println("$item 不是可装备的物品")
            }
        }
    }

    // 批量食用物品
    private fun edibleItems(selectedItems: List<String>) {
        var healthRestored = 0
        val itemsToRemove = mutableListOf<String>()
        var curedPoison = false

        selectedItems.forEach { item ->
            when (item) {
                "苹果" -> {
                    healthRestored += 20
                    itemsToRemove.add(item)
                }

                "面包" -> {
                    healthRestored += 30
                    itemsToRemove.add(item)
                }

                "草药" -> {
                    healthRestored += 40
                    curedPoison = true
                    itemsToRemove.add(item)
                }

                "烈酒" -> {
                    healthRestored += 50
                    player.inventory.remove(item)
                }
            }
        }

        if (healthRestored > 0) {
            itemsToRemove.forEach { item ->
                player.inventory.remove(item)
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
    private fun discardItems(selectedItems: List<String>) {
        selectedItems.forEach { item ->
            player.inventory.remove(item)
        }
        currentLocation.items.addAll(selectedItems)
        println("你丢弃了: ${selectedItems.joinToString(", ")}")
    }

    // 返回村庄功能
    private fun returnToVillage() {
        val village = locations["村庄"] ?: return
        currentLocation = village
        println("你使用传送卷轴回到了村庄！")

        // 检查是否可以进行休息
        checkRestInVillage()
    }

    // 村庄休息功能
    private fun checkRestInVillage() {
        if (currentLocation.name != "村庄") return

        val currentTime = System.currentTimeMillis()
        val timeSinceLastRest = currentTime - player.lastRestTime

        if (timeSinceLastRest >= player.restCooldown) {
            player.health = (player.health + player.restHealAmount).coerceAtMost(player.maxHealth)
            player.lastRestTime = currentTime
            println("你在村庄休息了一会，恢复了${player.restHealAmount}点生命值。")
            println("当前生命值: ${player.health}/${player.maxHealth}")
        } else {
            val remainingTime = (player.restCooldown - timeSinceLastRest) / 1000
            println("你还太疲惫，需要等待${remainingTime}秒才能再次休息。")
        }
    }

    private fun checkAllQuestsCompleted(): Boolean {
        return player.completedQuests.containsAll(listOf(banditQuestName, "寻找木剑", "收集矿石", "钓鱼"))
    }

    private fun triggerFinalBossBattle() {
        println("\n=== 最终决战 ===")
        println("$FINAL_BOSS_NAME 向你发起了挑战！")
        println("它的体型巨大，浑身散发着黑暗能量，眼睛如血般鲜红！")

        while (finalBossHealth > 0 && player.health > 0) {
            // 显示Boss状态
            val healthPercentage = (finalBossHealth.toDouble() / FINAL_BOSS_MAX_HEALTH * 100).toInt()
            println("\n$FINAL_BOSS_NAME 生命值: $finalBossHealth/$FINAL_BOSS_MAX_HEALTH ($healthPercentage%) | 阶段: $finalBossPhase/3")
            println("你的生命值: ${player.health}/${player.maxHealth}")

            // Boss阶段转换
            when {
                finalBossHealth < FINAL_BOSS_MAX_HEALTH / 3 && finalBossPhase == 1 -> {
                    finalBossPhase = 2
                    println("\n$FINAL_BOSS_NAME 暴怒了！它的攻击变得更加凶猛！")
                }

                finalBossHealth < FINAL_BOSS_MAX_HEALTH / 10 && finalBossPhase == 2 -> {
                    finalBossPhase = 3
                    println("\n$FINAL_BOSS_NAME 进入绝望模式！它开始释放全部黑暗能量！")
                }
            }

            // 玩家行动选择
            println("\n1. 攻击 | 2. 使用物品 | 3. 尝试逃跑")
            when (readlnOrNull()) {
                "1" -> attackFinalBoss()
                "2" -> {
                    openInventory()
                    if (player.health <= 0) break
                }

                "3" -> {
                    if (attemptEscapeFromBoss()) {
                        println("你成功逃离了战斗！但$FINAL_BOSS_NAME 仍在洞穴深处...")
                        finalBattleTriggered = false
                        return
                    } else {
                        println("逃跑失败！$FINAL_BOSS_NAME 挡住了你的去路！")
                    }
                }

                else -> println("无效输入，自动选择攻击！")
            }

            // Boss行动
            if (finalBossHealth > 0) {
                bossAttack()
            }
        }

        // 战斗结果处理
        if (finalBossHealth <= 0) {
            finalBossDefeated = true
            shouldWarnAboutBoss = false  // 更新状态
            println("\n=== 胜利 ===")
            println("经过一场史诗般的战斗，你终于击败了$FINAL_BOSS_NAME！")
            // 添加奖励
            player.gold += 10000
            player.inventory.add("王者之剑")
            player.inventory.add("皇家护甲")
            player.inventory.add("公主的戒指")

            println("\n你获得了:")
            println("10000 金币")
            println("王者之剑")
            println("皇家护甲")
            println("公主的戒指")

            Thread.sleep(2000)
            println("\n洞穴深处传来微弱的呼救声...")
            Thread.sleep(1500)
            println("你发现被囚禁的公主！")
            Thread.sleep(1000)
            println("\n公主: 勇敢的冒险者，你救了我！王国将永远铭记你的功绩！")
            princessRescued = true
            Thread.sleep(1500)
            println("\n=== 游戏通关 ===")
            println("你和公主回到了王国，举行了盛大的婚礼。")
            println("你们幸福地生活在一起，王国也恢复了和平。")
            Thread.sleep(2000)
            println("\n感谢游玩！游戏结束。")
            exitProcess(0)
        } else {
            println("\n你被$FINAL_BOSS_NAME 击败了...")
            println("黑暗笼罩了王国...")
            exitProcess(0)
        }
    }

    private fun attackFinalBoss() {
        val baseDamage = calculatePlayerDamage()

        // 根据阶段调整伤害
        val damageMultiplier = when (finalBossPhase) {
            1 -> 1.0
            2 -> 0.8
            3 -> 0.5
            else -> 1.0
        }

        val actualDamage = (baseDamage * damageMultiplier).toInt()
        finalBossHealth -= actualDamage
        println("你对$FINAL_BOSS_NAME 造成了 $actualDamage 点伤害！")

        // Boss自动回血
        if (Random.nextInt(100) < 30) {
            val healAmount = Random.nextInt(100, 300)
            finalBossHealth = (finalBossHealth + healAmount).coerceAtMost(FINAL_BOSS_MAX_HEALTH)
            println("$FINAL_BOSS_NAME 的黑暗能量使它恢复了 $healAmount 点生命值！")
        }
    }

    private fun bossAttack() {
        val attackType = when (Random.nextInt(100)) {
            in 0..60 -> "普通攻击"
            in 61..85 -> "吸血攻击"
            else -> "黑暗冲击波"
        }

        val damage = when (attackType) {
            "普通攻击" -> Random.nextInt(50, 100) * finalBossPhase
            "吸血攻击" -> {
                val dmg = Random.nextInt(80, 150) * finalBossPhase
                val heal = dmg / 2
                finalBossHealth = (finalBossHealth + heal).coerceAtMost(FINAL_BOSS_MAX_HEALTH)
                println("$FINAL_BOSS_NAME 从你身上吸取了生命值，恢复了 $heal 点生命！")
                dmg
            }

            "黑暗冲击波" -> {
                println("$FINAL_BOSS_NAME 释放了黑暗冲击波！")
                Random.nextInt(150, 250) * finalBossPhase
            }

            else -> 0
        }

        val actualDamage = (damage - player.defenseBonus).coerceAtLeast(10)
        player.health -= actualDamage
        println("$FINAL_BOSS_NAME 的$attackType 对你造成了 $actualDamage 点伤害！")
    }

    private fun attemptEscapeFromBoss(): Boolean {
        return Random.nextFloat() < 0.2f
    }

    companion object {
        private const val FINAL_BOSS_NAME = "暗影魔王卡奥斯"
        private const val FINAL_BOSS_MAX_HEALTH = 10000
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