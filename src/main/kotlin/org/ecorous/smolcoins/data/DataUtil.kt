package org.ecorous.smolcoins.data

import dev.emi.emi.api.recipe.EmiRecipe
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.registry.Registries
import net.minecraft.registry.tag.TagKey
import net.minecraft.util.Identifier
import org.ecorous.smolcoins.block.SmolcoinExchangeEmiRecipe
import org.quiltmc.loader.api.minecraft.ClientOnly

object DataUtil {

}
data class StackOrTag(val item: Item?, val tag: TagKey<Item>?, val count: Int) {
    fun take(other: ItemStack): Int {
        if(item != null && !other.isOf(this.item)) return 0
        if(tag != null && !other.isIn(this.tag)) return 0
        if(other.count < this.count) return 0
        return this.count
    }
    companion object {
        fun of(str: String): StackOrTag {
            val (ident, num) = if(str.contains('*')) str.split('*') else listOf(str, "1")
            val count = num.toIntOrNull()?.coerceIn(1..64) ?: 1
            return if(ident.startsWith('#')) {
                StackOrTag(null, TagKey.of(Registries.ITEM.key, Identifier(ident.substring(1))), count)
            } else {
                StackOrTag(Registries.ITEM[Identifier(ident)], null, count)
            }
        }
    }
}
