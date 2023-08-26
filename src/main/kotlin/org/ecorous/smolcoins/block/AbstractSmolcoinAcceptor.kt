package org.ecorous.smolcoins.block

import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.network.listener.ClientPlayPacketListener
import net.minecraft.network.packet.Packet
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket
import net.minecraft.registry.Registries
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.ItemScatterer
import net.minecraft.util.collection.DefaultedList
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import org.ecorous.smolcoins.item.SmolcoinItem
import org.ecorous.smolcoins.item.SmolcoinKeyItem
import org.ecorous.smolcoins.item.SmolcoinsItems
import java.util.*

abstract class AbstractSmolcoinAcceptorBlockEntity<T : BlockEntity>(type: BlockEntityType<T>, pos: BlockPos, state: BlockState) : BlockEntity(type, pos, state) {
    var keyId: UUID = UUID.randomUUID()
    var storedCoins = 0
    var cachedCoins = 0
    var activateCoins = 0
    var locked = false
    override fun readNbt(nbt: NbtCompound) {
        super.readNbt(nbt)
        keyId = nbt.getUuid("Key")
        storedCoins = nbt.getInt("StoredCoins")
        cachedCoins = nbt.getInt("CachedCoins")
        activateCoins = nbt.getInt("ActivateCoins")
        locked = nbt.getBoolean("Locked")
    }
    override fun writeNbt(nbt: NbtCompound) {
        nbt.putBoolean("Locked", locked)
        nbt.putInt("ActivateCoins", activateCoins)
        nbt.putInt("CachedCoins", cachedCoins)
        nbt.putInt("StoredCoins", storedCoins)
        nbt.putUuid("Key", keyId)
        super.writeNbt(nbt)
    }
    fun createKey(): ItemStack {
        val stack = ItemStack(SmolcoinKeyItem)
        val nbt = stack.getOrCreateNbt()
        nbt.putUuid("Key", keyId)
        nbt.putString("Holder", Registries.BLOCK.getId(cachedState.block).toString())
        nbt.putIntArray("Pos", listOf(pos.x, pos.y, pos.z))
        return stack
    }


    abstract fun onActivated(state: BlockState?, world: World?, pos: BlockPos, player: PlayerEntity?, hand: Hand?, hit: BlockHitResult?): ActionResult
    override fun toUpdatePacket(): Packet<ClientPlayPacketListener>? {
        return BlockEntityUpdateS2CPacket.of(this)
    }

    override fun toSyncedNbt(): NbtCompound {
        return toNbt()
    }
}
interface AbstractSmolcoinAcceptorBlock {
    fun onUse(
        state: BlockState?,
        world: World?,
        pos: BlockPos?,
        player: PlayerEntity?,
        hand: Hand?,
        hit: BlockHitResult?
    ): ActionResult {
        world ?: return ActionResult.PASS
        pos ?: return ActionResult.PASS

        val stack = player?.getStackInHand(hand) ?: return ActionResult.PASS
        val entity = world.getBlockEntity(pos) as? AbstractSmolcoinAcceptorBlockEntity<*> ?: return ActionResult.PASS

        if(entity.locked) {
            if(stack.item == SmolcoinKeyItem && stack.nbt!!.getUuid("Key") == entity.keyId) {
                if(!world.isClient) {
                    player.sendMessage(Text.translatable("block.smolcoins.pos.kquery", entity.cachedCoins, entity.storedCoins, entity.activateCoins), true)
                    val coinItems = SmolcoinItem.smolcoinsToItems(entity.storedCoins)
                    for(coin in coinItems) {
                        if(!player.giveItemStack(coin)) ItemScatterer.spawn(world, pos, DefaultedList.copyOf(ItemStack.EMPTY, coin))
                    }
                    player.sendMessage(Text.translatable("block.smolcoins.pos.activate", entity.storedCoins), true)
                    entity.storedCoins = 0
                }
                entity.onActivated(state, world, pos, player, hand, hit)
                return ActionResult.SUCCESS
            } else if(stack.isIn(SmolcoinsItems.smolcoinTag)) {
                if(!world.isClient) {
                    val value = SmolcoinItem.itemsToSmolcoins(stack.item.defaultStack)
                    entity.cachedCoins += value
                    stack.decrement(1)
                }
                println("Cached: " + entity.cachedCoins + "Lim: " + entity.activateCoins)
                if(entity.cachedCoins >= entity.activateCoins) {
                    if(!world.isClient) {
                        entity.storedCoins += entity.activateCoins
                        entity.cachedCoins -= entity.activateCoins
                        val coins = SmolcoinItem.smolcoinsToItems(entity.cachedCoins)
                        for(coin in coins) {
                            if(!player.giveItemStack(coin)) ItemScatterer.spawn(world, pos, DefaultedList.copyOf(ItemStack.EMPTY, coin))
                        }
                        player.sendMessage(Text.translatable("block.smolcoins.pos.activate", entity.cachedCoins), true)
                        entity.cachedCoins = 0
                    }
                    return entity.onActivated(state, world, pos, player, hand, hit)
                }
                return ActionResult.CONSUME
            } else {
                if(!world.isClient) {
                    val coinItems = SmolcoinItem.smolcoinsToItems(entity.cachedCoins)
                    for(coin in coinItems) {
                        if(!player.giveItemStack(coin)) ItemScatterer.spawn(world, pos, DefaultedList.copyOf(ItemStack.EMPTY, coin))
                    }
                    entity.cachedCoins = 0
                    player.sendMessage(Text.translatable("block.smolcoins.pos.query", entity.activateCoins), true)
                }
                return ActionResult.SUCCESS
            }
        } else {
            if(stack.item == SmolcoinKeyItem && stack.nbt?.getUuid("Key") == entity.keyId) {
                if(entity.activateCoins > 0) {
                    if(!world.isClient) {
                        entity.locked = true
                        player.sendMessage(Text.translatable("block.smolcoins.pos.lock", entity.activateCoins), true)
                    }
                    return ActionResult.SUCCESS
                }
                return ActionResult.PASS
            } else if(stack.isIn(SmolcoinsItems.smolcoinTag)) {
                if(!world.isClient) {
                    val value = SmolcoinItem.itemsToSmolcoins(stack.item.defaultStack)
                    entity.activateCoins += value
                    player.sendMessage(Text.translatable("block.smolcoins.pos.increase", entity.activateCoins), true)
                }
                return ActionResult.SUCCESS
            } else {
                if(!world.isClient) {
                    player.sendMessage(Text.translatable("block.smolcoins.pos.query", entity.activateCoins), true)
                }
                return ActionResult.PASS
            }
        }
    }
    fun onPlaced(
        world: World?,
        pos: BlockPos?,
        state: BlockState?,
        placer: LivingEntity?,
        itemStack: ItemStack?
    ) {
        val entity = world?.getBlockEntity(pos) as? AbstractSmolcoinAcceptorBlockEntity<*> ?: return
        if((placer as? PlayerEntity)?.giveItemStack(entity.createKey()) == false) ItemScatterer.spawn(world, pos, DefaultedList.copyOf(ItemStack.EMPTY, entity.createKey()))
    }
}
