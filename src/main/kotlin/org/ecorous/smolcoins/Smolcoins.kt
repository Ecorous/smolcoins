package org.ecorous.smolcoins

import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.util.Identifier
import net.minecraft.util.collection.DefaultedList
import org.quiltmc.loader.api.ModContainer
import org.quiltmc.qsl.base.api.entrypoint.ModInitializer
import org.quiltmc.qsl.item.setting.api.QuiltItemSettings
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object Smolcoins : ModInitializer {
    fun id(id: String): Identifier {
        return Identifier("smolcoins", id)
    }

    val SMOLCOIN_CONVERSION = mapOf(
        Identifier("minecraft", "copper_block") to 5,
        Identifier("minecraft", "coal") to 1,
        Identifier("minecraft", "diamond") to 45,
        Identifier("minecraft", "iron_ingot") to 5,
        Identifier("minecraft", "gold_ingot") to 5,
        Identifier("minecraft", "netherite_ingot") to 100,
    )
    val SMOLCOIN_SETTINGS = QuiltItemSettings()
    val LOGGER: Logger = LoggerFactory.getLogger("smolcoins")

    val smolcoin_1: Item = Item(SMOLCOIN_SETTINGS)
    val smolcoin_5: Item = Item(SMOLCOIN_SETTINGS)
    val smolcoin_10: Item = Item(SMOLCOIN_SETTINGS)
    val smolcoin_25: Item = Item(SMOLCOIN_SETTINGS)
    val smolcoin_50: Item = Item(SMOLCOIN_SETTINGS)
    val smolcoin_100: Item = Item(SMOLCOIN_SETTINGS)

    override fun onInitialize(mod: ModContainer) {
        Registry.register(Registries.ITEM, id("smolcoin_1"), smolcoin_1)
        Registry.register(Registries.ITEM, id("smolcoin_5"), smolcoin_5)
        Registry.register(Registries.ITEM, id("smolcoin_10"), smolcoin_10)
        Registry.register(Registries.ITEM, id("smolcoin_25"), smolcoin_25)
        Registry.register(Registries.ITEM, id("smolcoin_50"), smolcoin_50)
        Registry.register(Registries.ITEM, id("smolcoin_100"), smolcoin_100)
        LOGGER.info("Hello Quilt world from {}!", mod.metadata()?.name())
    }

    fun smolcoinsToItems(smolcoins: Int): Array<ItemStack> {
        val items = mutableListOf<ItemStack>()

        var remainingSmolcoins = smolcoins
        while (remainingSmolcoins >= 100) {
            items.add(smolcoin_100.defaultStack)
            remainingSmolcoins -= 100
        }
        while (remainingSmolcoins >= 50) {
            items.add(smolcoin_50.defaultStack)
            remainingSmolcoins -= 50
        }
        while (remainingSmolcoins >= 25) {
            items.add(smolcoin_25.defaultStack)
            remainingSmolcoins -= 25
        }
        while (remainingSmolcoins >= 10) {
            items.add(smolcoin_10.defaultStack)
            remainingSmolcoins -= 10
        }
        while (remainingSmolcoins >= 5) {
            items.add(smolcoin_5.defaultStack)
            remainingSmolcoins -= 5
        }
        while (remainingSmolcoins >= 1) {
            items.add(smolcoin_1.defaultStack)
            remainingSmolcoins -= 1
        }

        return items.groupBy { it.item }.map { (_, itemStacks) ->
            itemStacks.reduce { acc, itemStack -> acc.also { it.count += itemStack.count } }
        }.toTypedArray()
    }

    fun getNextEmptySlot(inventory: DefaultedList<ItemStack>): Int? {
        for ((index, item) in inventory.withIndex()) {
            if (item == null || item.isEmpty) {
                return index
            }
        }
        return null
    }

}
