package org.ecorous.smolcoins.item

import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import org.quiltmc.qsl.item.setting.api.QuiltItemSettings

data class SmolcoinItem(val value: Int) : Item(QuiltItemSettings()) {
    companion object {
        fun smolcoinsToItems(smolcoins: Int): Array<ItemStack> {
            val items = mutableListOf<ItemStack>()

            var remainingSmolcoins = smolcoins

            val smolcoins100 = remainingSmolcoins / 100
            items.add(ItemStack(SmolcoinsItems.smolcoin100, smolcoins100))
            remainingSmolcoins -= smolcoins100 * 100

            val smolcoins50 = remainingSmolcoins / 50
            items.add(ItemStack(SmolcoinsItems.smolcoin50, smolcoins50))
            remainingSmolcoins -= smolcoins50 * 50

            val smolcoins25 = remainingSmolcoins / 25
            items.add(ItemStack(SmolcoinsItems.smolcoin25, smolcoins25))
            remainingSmolcoins -= smolcoins25 * 25

            val smolcoins10 = remainingSmolcoins / 10
            items.add(ItemStack(SmolcoinsItems.smolcoin10, smolcoins10))
            remainingSmolcoins -= smolcoins10 * 10

            val smolcoins5 = remainingSmolcoins / 5
            items.add(ItemStack(SmolcoinsItems.smolcoin5, smolcoins5))
            remainingSmolcoins -= smolcoins5 * 5

            items.add(ItemStack(SmolcoinsItems.smolcoin1, remainingSmolcoins))

            return items.toTypedArray()
        }
        fun itemsToSmolcoins(vararg items: ItemStack): Int {
            var count = 0
            for(stack in items.filter { i -> i.item is SmolcoinItem }) {
                count += stack.count * (stack.item as SmolcoinItem).value
            }
            return count
        }
    }
}
