package org.ecorous.smolcoins

import com.mojang.blaze3d.systems.RenderSystem
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
import net.minecraft.inventory.Inventories
import net.minecraft.inventory.Inventory
import net.minecraft.inventory.SimpleInventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.screen.NamedScreenHandlerFactory
import net.minecraft.screen.ScreenHandler
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
import net.minecraft.world.World
import org.quiltmc.loader.api.minecraft.ClientOnly
import org.quiltmc.qsl.block.extensions.api.QuiltBlockSettings

object SmolcoinExchangeBlock : BlockWithEntity(QuiltBlockSettings.create().sounds(BlockSoundGroup.WOOD).strength(Float.MAX_VALUE)) {
    override fun createBlockEntity(pos: BlockPos, state: BlockState) = SmolcoinExchangeBlockEntity(pos, state)
    override fun <T : BlockEntity?> getTicker(
        world: World?,
        state: BlockState?,
        type: BlockEntityType<T>?
    ) = checkType(type, Smolcoins.exchangeBlockEntity) { world1, pos, _, be -> be.tick(world1, pos) }
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
class SmolcoinExchangeBlockEntity(pos: BlockPos, state: BlockState) : BlockEntity(Smolcoins.exchangeBlockEntity, pos, state), InventoryImpl,
    NamedScreenHandlerFactory {
    override var inventory: DefaultedList<ItemStack> = DefaultedList.ofSize(7, ItemStack.EMPTY)
    fun tick(world: World, pos: BlockPos) {
        if(world.isClient) return
        if(inventory[0].isEmpty) return
        val coinCount = Smolcoins.SMOLCOIN_CONVERSION[inventory[0].item] ?: 0
        if(coinCount == 0) return
        val coinsToDispense = Smolcoins.smolcoinsToItems(coinCount)
        println(coinsToDispense.size)
        val slotMap = hashMapOf(
            Smolcoins.smolcoin_1 to 6,
            Smolcoins.smolcoin_5 to 5,
            Smolcoins.smolcoin_10 to 4,
            Smolcoins.smolcoin_25 to 3,
            Smolcoins.smolcoin_50 to 2,
            Smolcoins.smolcoin_100 to 1
        )
        for(coin in coinsToDispense) {
            if(!tryInsert(slotMap[coin.item] ?: -1, coin)) {
                world.spawnEntity(ItemEntity(world, pos.x.toDouble() + 0.5, pos.y.toDouble() + 1.0, pos.z.toDouble() + 0.5, coin))
            }
        }
        inventory[0].decrement(1)
    }

    fun tryInsert(index: Int, stack: ItemStack): Boolean {
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

    override fun getDisplayName() = Text.translatable("block.smolcoins.exchange")
}
class SmolcoinExchangeScreenHandler(syncId: Int, playerInventory: PlayerInventory, private val inventory: Inventory) :
    ScreenHandler(Smolcoins.exchangeScreenHandlerType, syncId) {
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
        private val TEXTURE = Identifier("smolcoins:textures/gui/exchange.png")
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
interface InventoryImpl : Inventory {
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
}
