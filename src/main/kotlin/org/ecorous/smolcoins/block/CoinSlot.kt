package org.ecorous.smolcoins.block

import net.fabricmc.fabric.api.`object`.builder.v1.block.type.BlockSetTypeRegistry
import net.minecraft.block.*
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityTicker
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.block.enums.WallMountLocation
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.network.listener.ClientPlayPacketListener
import net.minecraft.network.packet.Packet
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket
import net.minecraft.registry.Registries
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvent
import net.minecraft.sound.SoundEvents
import net.minecraft.state.StateManager
import net.minecraft.state.property.BooleanProperty
import net.minecraft.state.property.Properties
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.shape.VoxelShape
import net.minecraft.util.shape.VoxelShapes
import net.minecraft.world.BlockView
import net.minecraft.world.World
import net.minecraft.world.WorldAccess
import org.ecorous.smolcoins.init.SmolcoinsInit
import org.quiltmc.qkl.library.registry.invoke
import org.quiltmc.qsl.block.entity.api.QuiltBlockEntityTypeBuilder
import org.quiltmc.qsl.block.extensions.api.QuiltBlockSettings

/*
WARNING: this code has been known in...us...to cause insanity...
it seems to be some sort of problem in serializing or deserializing the NBT,
because every time the game is restarted parts of the block data that are updated in AbstractSmolcoinAcceptorBlock#onUse
are reset to their default values. it only does this sometimes. i have NO clue why it does this.
also the texture sucks but whatever that was expected
-éowyn
 */
object CoinSlot {
    val coinSlotBlock = CoinSlotBlock()
    val coinSlotBlockEntity: BlockEntityType<CoinSlotBlockEntity> = QuiltBlockEntityTypeBuilder.create({ pos, state ->
        CoinSlotBlockEntity(pos, state)
    }, coinSlotBlock).build()
    internal fun init() {
        Registries.BLOCK {
            coinSlotBlock withId SmolcoinsInit.id("coin_slot")
        }
        Registries.BLOCK_ENTITY_TYPE {
            coinSlotBlockEntity withId SmolcoinsInit.id("coin_slot")
        }
    }
}
class CoinSlotBlock : WallMountedBlock(QuiltBlockSettings.create()), BlockEntityProvider, AbstractSmolcoinAcceptorBlock {
    companion object {
        val POWERED: BooleanProperty = Properties.POWERED
    }
    private var onTicks = 0
    override fun appendProperties(builder: StateManager.Builder<Block, BlockState>) {
        builder.add(FACE, HorizontalFacingBlock.FACING, POWERED)
    }

    override fun onPlaced(
        world: World?,
        pos: BlockPos?,
        state: BlockState?,
        placer: LivingEntity?,
        itemStack: ItemStack?
    ) {
        super<WallMountedBlock>.onPlaced(world, pos, state, placer, itemStack)
        super<AbstractSmolcoinAcceptorBlock>.onPlaced(world, pos, state, placer, itemStack)
    }
    override fun onUse(
        state: BlockState?,
        world: World?,
        pos: BlockPos?,
        player: PlayerEntity?,
        hand: Hand?,
        hit: BlockHitResult?
    ): ActionResult {
        val res = super<AbstractSmolcoinAcceptorBlock>.onUse(state, world, pos, player, hand, hit)
        updateNeighbors(state!!, world!!, pos!!)
        world.updateListeners(pos, state, state, Block.NOTIFY_LISTENERS)
        return res
    }

    override fun getRenderType(state: BlockState?) = BlockRenderType.MODEL
    override fun getCollisionShape(
        state: BlockState,
        world: BlockView?,
        pos: BlockPos?,
        context: ShapeContext?
    ): VoxelShape {
        return VoxelShapes.empty()
    }
    override fun getOutlineShape(
        state: BlockState,
        world: BlockView?,
        pos: BlockPos?,
        context: ShapeContext?
    ): VoxelShape {
        return when(state[FACE]) {
            WallMountLocation.WALL -> when(state[FACING]) {
                Direction.NORTH -> VoxelShapes.cuboid(5.0/16, 4.0/16, 15.0/16, 11.0/16, 12.0/16, 16.0/16)
                Direction.EAST -> VoxelShapes.cuboid(0.0/16, 4.0/16, 5.0/16, 1.0/16, 12.0/16, 11.0/16)
                Direction.SOUTH -> VoxelShapes.cuboid(5.0/16, 4.0/16, 0.0/16, 11.0/16, 12.0/16, 1.0/16)
                Direction.WEST -> VoxelShapes.cuboid(15.0/16, 4.0/16, 5.0/16, 16.0/16, 12.0/16, 11.0/16)
                else -> VoxelShapes.fullCube()
            }
            WallMountLocation.FLOOR -> when(state[FACING]) {
                Direction.NORTH -> VoxelShapes.cuboid(5.0/16, 0.0/16, 4.0/16, 11.0/16, 1.0/16, 12.0/16)
                Direction.EAST -> VoxelShapes.cuboid(4.0/16, 0.0/16, 5.0/16, 12.0/16, 1.0/16, 11.0/16)
                Direction.SOUTH -> VoxelShapes.cuboid(5.0/16, 0.0/16, 4.0/16, 11.0/16, 1.0/16, 12.0/16)
                Direction.WEST -> VoxelShapes.cuboid(4.0/16, 0.0/16, 5.0/16, 12.0/16, 1.0/16, 11.0/16)
                else -> VoxelShapes.fullCube()
            }
            WallMountLocation.CEILING -> when(state[FACING]) {
                Direction.NORTH -> VoxelShapes.cuboid(5.0/16, 15.0/16, 4.0/16, 11.0/16, 16.0/16, 12.0/16)
                Direction.EAST -> VoxelShapes.cuboid(4.0/16, 15.0/16, 5.0/16, 12.0/16, 16.0/16, 11.0/16)
                Direction.SOUTH -> VoxelShapes.cuboid(5.0/16, 15.0/16, 4.0/16, 11.0/16, 16.0/16, 12.0/16)
                Direction.WEST -> VoxelShapes.cuboid(4.0/16, 15.0/16, 5.0/16, 12.0/16, 16.0/16, 11.0/16)
                else -> VoxelShapes.fullCube()
            }
            else -> VoxelShapes.fullCube()
        }
    }

    override fun createBlockEntity(pos: BlockPos, state: BlockState) = CoinSlotBlockEntity(pos, state)
    private fun updateNeighbors(state: BlockState, world: World, pos: BlockPos) {
        world.updateNeighborsAlways(pos, this)
        world.updateNeighborsAlways(pos.offset(getDirection(state).opposite), this)
    }
    override fun getWeakRedstonePower(
        state: BlockState,
        world: BlockView?,
        pos: BlockPos?,
        direction: Direction?
    ): Int {
        return if (state.get(POWERED) as Boolean) 15 else 0
    }

    override fun getStrongRedstonePower(
        state: BlockState,
        world: BlockView?,
        pos: BlockPos?,
        direction: Direction
    ): Int {
        return if (state.get(POWERED) as Boolean && getDirection(state) == direction) 15 else 0
    }
    private fun powerOn(state: BlockState, world: World, pos: BlockPos) {
        world.setBlockState(pos, state.with(POWERED, true), 3)
        this.updateNeighbors(state, world, pos)
        world.scheduleBlockTick(pos, this, this.onTicks)
    }
    fun playClickSound(player: PlayerEntity?, world: WorldAccess, pos: BlockPos?, powered: Boolean) {
        world.playSound(if (powered) player else null, pos, getClickSound(powered), SoundCategory.BLOCKS)
    }

    fun getClickSound(powered: Boolean): SoundEvent {
        return if (powered) SoundEvents.BLOCK_STONE_BUTTON_CLICK_ON else SoundEvents.BLOCK_STONE_BUTTON_CLICK_OFF
    }
}
class CoinSlotBlockEntity(pos: BlockPos, state: BlockState) : AbstractSmolcoinAcceptorBlockEntity<CoinSlotBlockEntity>(CoinSlot.coinSlotBlockEntity, pos, state) {

    override fun onActivated(
        state: BlockState?,
        world: World?,
        pos: BlockPos,
        player: PlayerEntity?,
        hand: Hand?,
        hit: BlockHitResult?
    ): ActionResult {
        world ?: return ActionResult.PASS
        SmolcoinsInit.LOGGER.info("Activated")
        (state?.block as? CoinSlotBlock ?: return ActionResult.PASS).playClickSound(player, world, pos, true)
        return ActionResult.CONSUME
    }

    override fun toUpdatePacket(): Packet<ClientPlayPacketListener>? {
        return BlockEntityUpdateS2CPacket.of(this)
    }

    override fun toSyncedNbt(): NbtCompound {
        return toNbt()
    }
}
