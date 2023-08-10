package org.ecorous.smolcoins

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup
import net.minecraft.item.BlockItem
import net.minecraft.item.Item
import net.minecraft.item.ItemGroup
import net.minecraft.registry.Registries
import net.minecraft.registry.tag.TagKey
import net.minecraft.text.Text
import org.ecorous.smolcoins.block.SmolcoinExchangeBlock
import org.ecorous.smolcoins.init.SmolcoinsInit
import org.quiltmc.qkl.library.registry.*
import org.quiltmc.qsl.item.setting.api.QuiltItemSettings

object SmolcoinsItems {
    private val SMOLCOIN_SETTINGS = QuiltItemSettings()

    val smolcoin_1: Item = Item(SMOLCOIN_SETTINGS)
    val smolcoin_5: Item = Item(SMOLCOIN_SETTINGS)
    val smolcoin_10: Item = Item(SMOLCOIN_SETTINGS)
    val smolcoin_25: Item = Item(SMOLCOIN_SETTINGS)
    val smolcoin_50: Item = Item(SMOLCOIN_SETTINGS)
    val smolcoin_100: Item = Item(SMOLCOIN_SETTINGS)
    val exchangeBlockItem = BlockItem(SmolcoinExchangeBlock, QuiltItemSettings())

    val smolcoinTag: TagKey<Item> = TagKey.of(Registries.ITEM.key, SmolcoinsInit.id("smolcoins"))

    val itemGroup: ItemGroup = FabricItemGroup.builder()
        .icon { smolcoin_100.defaultStack }
        .name(Text.translatable("smolcoins.name"))
        .entries { _, entries ->
            entries.addItem(exchangeBlockItem)
            entries.addItem(smolcoin_1)
            entries.addItem(smolcoin_5)
            entries.addItem(smolcoin_10)
            entries.addItem(smolcoin_25)
            entries.addItem(smolcoin_50)
            entries.addItem(smolcoin_100)
        }
        .build()

    internal fun init() {
        Registries.ITEM {
            smolcoin_1 withId SmolcoinsInit.id("smolcoin_1")
            smolcoin_5 withId SmolcoinsInit.id("smolcoin_5")
            smolcoin_10 withId SmolcoinsInit.id("smolcoin_10")
            smolcoin_25 withId SmolcoinsInit.id("smolcoin_25")
            smolcoin_50 withId SmolcoinsInit.id("smolcoin_50")
            smolcoin_100 withId SmolcoinsInit.id("smolcoin_100")
            exchangeBlockItem withId SmolcoinsInit.id("exchange")
        }
        Registries.ITEM_GROUP {
            itemGroup withId SmolcoinsInit.id("smolcoins")
        }
    }

}
