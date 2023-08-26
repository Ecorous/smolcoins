package org.ecorous.smolcoins.item

import net.minecraft.client.item.TooltipContext
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.registry.Registries
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import org.quiltmc.qsl.item.setting.api.QuiltItemSettings

object SmolcoinKeyItem : Item(QuiltItemSettings().maxCount(1)) {
    override fun appendTooltip(
        stack: ItemStack?,
        world: World?,
        tooltip: MutableList<Text>,
        context: TooltipContext
    ) {
        val nbt = stack?.nbt ?: return
        val holder = Registries.BLOCK.get(Identifier(nbt.getString("Holder"))).name
        val posRaw = nbt.getIntArray("Pos")
        val pos = BlockPos(posRaw[0], posRaw[1], posRaw[2]).toShortString()
        tooltip.add(Text.literal(String.format(Text.translatable("item.smolcoins.key.sep").getString(), holder.getString(), pos)).formatted(
            Formatting.DARK_GRAY)
        )
    }
}
