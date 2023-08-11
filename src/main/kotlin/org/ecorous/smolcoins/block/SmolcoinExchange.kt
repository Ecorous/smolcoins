package org.ecorous.smolcoins.block

import com.mojang.blaze3d.systems.RenderSystem
import dev.emi.emi.api.EmiRegistry
import dev.emi.emi.api.recipe.EmiRecipe
import dev.emi.emi.api.recipe.EmiRecipeCategory
import dev.emi.emi.api.render.EmiTexture
import dev.emi.emi.api.stack.EmiIngredient
import dev.emi.emi.api.stack.EmiStack
import dev.emi.emi.api.widget.WidgetHolder
import net.minecraft.block.BlockRenderType
import net.minecraft.block.BlockState
import net.minecraft.block.BlockWithEntity
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.entity.ItemEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.feature_flags.FeatureFlags
import net.minecraft.inventory.Inventories
import net.minecraft.inventory.Inventory
import net.minecraft.inventory.SidedInventory
import net.minecraft.inventory.SimpleInventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.recipe.Ingredient
import net.minecraft.registry.Registries
import net.minecraft.screen.NamedScreenHandlerFactory
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.ScreenHandlerType
import net.minecraft.screen.slot.FurnaceOutputSlot
import net.minecraft.screen.slot.Slot
import net.minecraft.sound.BlockSoundGroup
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.Identifier
import net.minecraft.util.collection.DefaultedList
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.World
import org.ecorous.smolcoins.SmolcoinsItems
import org.ecorous.smolcoins.block.SmolcoinExchange.canAddCoins
import org.ecorous.smolcoins.block.SmolcoinExchange.exchangeBlockEntity
import org.ecorous.smolcoins.block.SmolcoinExchange.exchangeScreenHandlerType
import org.ecorous.smolcoins.block.SmolcoinExchange.itemsToSmolcoins
import org.ecorous.smolcoins.block.SmolcoinExchange.smolcoinConversions
import org.ecorous.smolcoins.block.SmolcoinExchange.smolcoinsToItems
import org.ecorous.smolcoins.block.SmolcoinExchangeEmiRecipe.Companion.EMI_CATEGORY
import org.ecorous.smolcoins.block.SmolcoinExchangeEmiRecipe.Companion.EMI_WORKSTATION
import org.ecorous.smolcoins.init.SmolcoinsInit
import org.quiltmc.loader.api.minecraft.ClientOnly
import org.quiltmc.qkl.library.registry.invoke
import org.quiltmc.qsl.block.entity.api.QuiltBlockEntityTypeBuilder
import org.quiltmc.qsl.block.extensions.api.QuiltBlockSettings

object SmolcoinExchange {

    var smolcoinConversions = mutableMapOf<Identifier, Int>()
    val exchangeBlockEntity: BlockEntityType<SmolcoinExchangeBlockEntity> = QuiltBlockEntityTypeBuilder.create({ pos, state ->
        SmolcoinExchangeBlockEntity(pos, state)
    }, SmolcoinExchangeBlock).build()
    val exchangeScreenHandlerType = ScreenHandlerType({ syncId, playerInventory ->
        SmolcoinExchangeScreenHandler(syncId, playerInventory)
    }, FeatureFlags.DEFAULT_SET)
    internal fun init() {
        Registries.BLOCK {
            SmolcoinExchangeBlock withId SmolcoinsInit.id("exchange")
        }
        Registries.BLOCK_ENTITY_TYPE {
            exchangeBlockEntity withId SmolcoinsInit.id("exchange")
        }
        Registries.SCREEN_HANDLER_TYPE {
            exchangeScreenHandlerType withId SmolcoinsInit.id("exchange")
        }
    }
    @ClientOnly
    internal fun initEmi(registry: EmiRegistry) {
        registry.addCategory(EMI_CATEGORY)
        registry.addWorkstation(EMI_CATEGORY, EMI_WORKSTATION)
        for(recipe in smolcoinConversions.map { entry -> SmolcoinExchangeEmiRecipe(entry.key, entry.value)}) {
            registry.addRecipe(recipe)
        }
    }
    fun smolcoinsToItems(smolcoins: Int): Array<ItemStack> {
        val items = mutableListOf<ItemStack>()

        var remainingSmolcoins = smolcoins

        val smolcoins100 = remainingSmolcoins / 100
        items.add(ItemStack(SmolcoinsItems.smolcoin_100, smolcoins100))
        remainingSmolcoins -= smolcoins100 * 100

        val smolcoins50 = remainingSmolcoins / 50
        items.add(ItemStack(SmolcoinsItems.smolcoin_50, smolcoins50))
        remainingSmolcoins -= smolcoins50 * 50

        val smolcoins25 = remainingSmolcoins / 25
        items.add(ItemStack(SmolcoinsItems.smolcoin_25, smolcoins25))
        remainingSmolcoins -= smolcoins25 * 25

        val smolcoins10 = remainingSmolcoins / 10
        items.add(ItemStack(SmolcoinsItems.smolcoin_10, smolcoins10))
        remainingSmolcoins -= smolcoins10 * 10

        val smolcoins5 = remainingSmolcoins / 5
        items.add(ItemStack(SmolcoinsItems.smolcoin_5, smolcoins5))
        remainingSmolcoins -= smolcoins5 * 5

        items.add(ItemStack(SmolcoinsItems.smolcoin_1, remainingSmolcoins))

        return items.toTypedArray()
    }
    fun itemsToSmolcoins(items: Array<ItemStack>): Int {
        val smolcoins100 = items.getOrNull(0)?.count ?: 0
        val smolcoins50 = items.getOrNull(1)?.count ?: 0
        val smolcoins25 = items.getOrNull(2)?.count ?: 0
        val smolcoins10 = items.getOrNull(3)?.count ?: 0
        val smolcoins5 = items.getOrNull(4)?.count ?: 0
        val smolcoins1 = items.getOrNull(5)?.count ?: 0
        return (smolcoins100 * 100) + (smolcoins50 * 50) + (smolcoins25 * 25) + (smolcoins10 * 10) + (smolcoins5 * 5) + smolcoins1
    }
    fun canAddCoins(coinCount: Int, existing: List<ItemStack>): Boolean {
        val coinItems = smolcoinsToItems(coinCount)
        for(i in coinItems.indices) {
            if(existing[i].count + coinItems[i].count > 64) return false
        }
        return true
    }
}
@ClientOnly
class SmolcoinExchangeEmiRecipe(val item: Identifier, val coins: Int) : EmiRecipe {
    override fun getCategory() = EMI_CATEGORY

    override fun getId() = SmolcoinsInit.id("/exchange/${item.namespace}/${item.path}")

    override fun getInputs() = mutableListOf(EmiIngredient.of(Ingredient.ofItems(Registries.ITEM.get(item))))

    override fun getOutputs() = smolcoinsToItems(coins).map(EmiStack::of) as MutableList<EmiStack>

    override fun getDisplayWidth() = 64

    override fun getDisplayHeight() = 64
    override fun supportsRecipeTree() = false
    override fun addWidgets(widgets: WidgetHolder) {
        with(widgets) {
            addSlot(inputs[0], 23, 24)
            val slots = listOf(23 to 3, 44 to 15, 44 to 36, 23 to 45, 2 to 36, 2 to 15)

            for(i in 0 until 6) {
                addTexture(EmiTexture(SmolcoinExchangeScreen.TEXTURE, 80, 14, 18, 18), slots[i].first, slots[i].second)
            }
            for(i in 0 until outputs.size) {
                if(outputs[i].isEmpty) continue
                addSlot(outputs[i], slots[i].first, slots[i].second)
            }
            addText(Text.literal(coins.toString()), 44, 4, 0, false)
            // i am 90% sure this has to loop through twice because for whatever reason get() on MutableList isn't nullable so i can't just use an elvis operator
        }
    }
    companion object {
        internal val EMI_WORKSTATION = EmiStack.of(SmolcoinsItems.exchangeBlockItem)
        internal val EMI_CATEGORY = EmiRecipeCategory(SmolcoinsInit.id("exchange"), EMI_WORKSTATION, EMI_WORKSTATION)
    }
}

object SmolcoinExchangeBlock : BlockWithEntity(QuiltBlockSettings.create().sounds(BlockSoundGroup.WOOD).strength(Float.MAX_VALUE)) {
    override fun createBlockEntity(pos: BlockPos, state: BlockState) = SmolcoinExchangeBlockEntity(pos, state)
    override fun <T : BlockEntity?> getTicker(
        world: World?,
        state: BlockState?,
        type: BlockEntityType<T>?
    ) = checkType(type, exchangeBlockEntity) { world1, pos, _, be -> be.tick(world1, pos) }
    override fun getRenderType(state: BlockState) = BlockRenderType.MODEL
    override fun onUse(
        state: BlockState?,
        world: World?,
        pos: BlockPos?,
        player: PlayerEntity?,
        hand: Hand?,
        hit: BlockHitResult?
    ): ActionResult {
        player?.openHandledScreen(world?.getBlockEntity(pos) as? SmolcoinExchangeBlockEntity)
        return ActionResult.SUCCESS
    }
}
class SmolcoinExchangeBlockEntity(pos: BlockPos, state: BlockState) : BlockEntity(exchangeBlockEntity, pos, state),
    SmolcoinExchangeInventory, SidedInventory,
    NamedScreenHandlerFactory {
    override var inventory: DefaultedList<ItemStack> = DefaultedList.ofSize(7, ItemStack.EMPTY)
    override fun getAvailableSlots(side: Direction?) = intArrayOf(0, 1, 2, 3, 4, 5, 6)

    fun tick(world: World, pos: BlockPos) {
        if(world.isClient) return
        if(inventory[0].isEmpty) return
        val coinCount = smolcoinConversions[Registries.ITEM.getId(inventory[0].item)] ?: 0
        if(coinCount == 0) return
        val containedCoins = inventory.subList(1, 7)
        if(!canAddCoins(coinCount, containedCoins)) return
        val coinsToDispense = smolcoinsToItems(coinCount)
        println(coinsToDispense.size)
        val slotMap = hashMapOf(
            SmolcoinsItems.smolcoin_1 to 6,
            SmolcoinsItems.smolcoin_5 to 5,
            SmolcoinsItems.smolcoin_10 to 4,
            SmolcoinsItems.smolcoin_25 to 3,
            SmolcoinsItems.smolcoin_50 to 2,
            SmolcoinsItems.smolcoin_100 to 1
        )
        for(coin in coinsToDispense) {
            if(!tryInsert(slotMap[coin.item] ?: -1, coin)) {
                world.spawnEntity(ItemEntity(world, pos.x.toDouble() + 0.5, pos.y.toDouble() + 1.0, pos.z.toDouble() + 0.5, coin))
            }
        }
        inventory[0].decrement(1)
        val allCoins = smolcoinsToItems(itemsToSmolcoins(inventory.subList(1, inventory.size).toTypedArray()))
        for(i in 0 until 6) {
            inventory[i + 1] = allCoins[i]
        }
    }

    private fun tryInsert(index: Int, stack: ItemStack): Boolean {
        if(index < 1) return false
        if(inventory[index].count + stack.count > 64) return false
        if(inventory[index].isEmpty) {
            inventory[index] = stack
            return true
        }
        inventory[index].increment(stack.count)
        return true
    }
    override fun readNbt(nbt: NbtCompound?) {
        super.readNbt(nbt)
        Inventories.readNbt(nbt, inventory)
    }

    override fun writeNbt(nbt: NbtCompound?) {
        Inventories.writeNbt(nbt, inventory)
        super.writeNbt(nbt)
    }

    override fun createMenu(syncId: Int, playerInventory: PlayerInventory, player: PlayerEntity?) = SmolcoinExchangeScreenHandler(syncId, playerInventory, this)

    override fun getDisplayName(): Text = Text.translatable("block.smolcoins.exchange")
}
class SmolcoinExchangeScreenHandler(syncId: Int, playerInventory: PlayerInventory, private val inventory: Inventory) :
    ScreenHandler(exchangeScreenHandlerType, syncId) {
    constructor(syncId: Int, playerInventory: PlayerInventory) : this(syncId, playerInventory, SimpleInventory(7))

    init {
        checkSize(inventory, 7)
        inventory.onOpen(playerInventory.player)
        addSlot(Slot(inventory, 0, 81, 36))
        addSlot(FurnaceOutputSlot(playerInventory.player, inventory, 1, 81, 15))
        addSlot(FurnaceOutputSlot(playerInventory.player, inventory, 2, 102, 27))
        addSlot(FurnaceOutputSlot(playerInventory.player, inventory, 3, 102, 48))
        addSlot(FurnaceOutputSlot(playerInventory.player, inventory, 4, 81, 57))
        addSlot(FurnaceOutputSlot(playerInventory.player, inventory, 5, 60, 48))
        addSlot(FurnaceOutputSlot(playerInventory.player, inventory, 6, 60, 27))

        for (i in 0..2) {
            for (j in 0..8) {
                addSlot(Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18))
            }
        }

        for (i in 0..8) {
            addSlot(Slot(playerInventory, i, 8 + i * 18, 142))
        }
    }
    override fun canUse(player: PlayerEntity?): Boolean {
        return this.inventory.canPlayerUse(player)
    }

    override fun quickTransfer(player: PlayerEntity?, fromIndex: Int): ItemStack? {
        var itemStack = ItemStack.EMPTY
        val slot = slots[fromIndex]
        if (slot.hasStack()) {
            val itemStack2 = slot.stack
            itemStack = itemStack2.copy()
            when(fromIndex) {
                in 0 until 7 -> {
                    if(!insertItem(itemStack2, 7, 43, false)) {
                        return ItemStack.EMPTY
                    }
                }
                in 7 until 34 -> {
                    if(!insertItem(itemStack2, 0, 1, false)) {
                        if(!insertItem(itemStack2, 34, 43, true)) {
                            return ItemStack.EMPTY
                        }
                    }
                }
                in 34 until 44 -> {
                    if(!insertItem(itemStack2, 0, 1, false)) {
                        if(!insertItem(itemStack2, 7, 33, false)) {
                            return ItemStack.EMPTY
                        }
                    }
                }
            }
            if (itemStack2.isEmpty) {
                slot.setStackByPlayer(ItemStack.EMPTY)
            } else {
                slot.markDirty()
            }
            if (itemStack2.count == itemStack.count) {
                return ItemStack.EMPTY
            }
            slot.onTakeItem(player, itemStack2)
        }
        return itemStack
    }

    override fun close(player: PlayerEntity?) {
        super.close(player)
        this.inventory.onClose(player)
    }
}
@ClientOnly
class SmolcoinExchangeScreen(handler: SmolcoinExchangeScreenHandler, inventory: PlayerInventory) : HandledScreen<SmolcoinExchangeScreenHandler>(handler, inventory, Text.translatable("block.smolcoins.exchange")) {
    companion object {
        val TEXTURE = Identifier("smolcoins:textures/gui/exchange.png")
    }
    override fun init() {
        super.init()
        titleX = (backgroundWidth - textRenderer.getWidth(title)) / 2
    }
    override fun render(graphics: GuiGraphics?, mouseX: Int, mouseY: Int, delta: Float) {
        renderBackground(graphics)
        super.render(graphics, mouseX, mouseY, delta)
        drawMouseoverTooltip(graphics, mouseX, mouseY)
    }
    override fun drawBackground(graphics: GuiGraphics?, delta: Float, mouseX: Int, mouseY: Int) {
        RenderSystem.setShaderTexture(0, TEXTURE)
        val i = (width - backgroundWidth) / 2
        val j = (height - backgroundHeight) / 2
        graphics?.drawTexture(TEXTURE, i, j, 0, 0, backgroundWidth, backgroundHeight)
    }
}
interface SmolcoinExchangeInventory : SidedInventory {
    val inventory: DefaultedList<ItemStack>
    override fun clear() {
        inventory.clear()
    }

    override fun size(): Int {
        return inventory.size
    }

    override fun isEmpty(): Boolean {
        return inventory.isEmpty()
    }

    override fun getStack(slot: Int): ItemStack {
        return inventory[slot]
    }

    override fun removeStack(slot: Int, amount: Int): ItemStack {
        val result = Inventories.splitStack(inventory, slot, amount)
        if(!result.isEmpty)
            markDirty()
        return result
    }

    override fun removeStack(slot: Int): ItemStack {
        return Inventories.removeStack(inventory, slot)
    }

    override fun setStack(slot: Int, stack: ItemStack) {
        inventory[slot] = stack
        if(stack.count > stack.maxCount)
            stack.count = stack.maxCount
    }

    override fun canPlayerUse(player: PlayerEntity?): Boolean {
        return true
    }

    override fun canInsert(slot: Int, stack: ItemStack?, dir: Direction?): Boolean {
        return slot == 0
    }

    override fun canExtract(slot: Int, stack: ItemStack, dir: Direction?): Boolean {
        return slot > 0 && stack.isIn(SmolcoinsItems.smolcoinTag)
    }
}
