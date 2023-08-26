package org.ecorous.smolcoins.item

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup
import net.minecraft.item.BlockItem
import net.minecraft.item.Item
import net.minecraft.item.ItemGroup
import net.minecraft.registry.Registries
import net.minecraft.registry.tag.TagKey
import net.minecraft.text.Text
import org.ecorous.smolcoins.block.CoinSlot.coinSlotBlock
import org.ecorous.smolcoins.block.CoinSlotBlock
import org.ecorous.smolcoins.block.SmolcoinExchangeBlock
import org.ecorous.smolcoins.init.SmolcoinsInit
import org.quiltmc.qkl.library.registry.*
import org.quiltmc.qsl.item.setting.api.QuiltItemSettings

object SmolcoinsItems {
    val smolcoin1 = SmolcoinItem(1)
    val smolcoin5 = SmolcoinItem(5)
    val smolcoin10 = SmolcoinItem(10)
    val smolcoin25 = SmolcoinItem(25)
    val smolcoin50 = SmolcoinItem(50)
    val smolcoin100 = SmolcoinItem(100)

    val exchangeBlockItem = BlockItem(SmolcoinExchangeBlock, QuiltItemSettings())
    val coinSlotBlockItem = BlockItem(coinSlotBlock, QuiltItemSettings())

    val smolcoinTag: TagKey<Item> = TagKey.of(Registries.ITEM.key, SmolcoinsInit.id("smolcoins"))

    val itemGroup: ItemGroup = FabricItemGroup.builder()
        .icon { smolcoin100.defaultStack }
        .name(Text.translatable("smolcoins.name"))
        .entries { _, entries ->
            entries.addItem(exchangeBlockItem)
            entries.addItem(coinSlotBlockItem)
            entries.addItem(smolcoin1)
            entries.addItem(smolcoin5)
            entries.addItem(smolcoin10)
            entries.addItem(smolcoin25)
            entries.addItem(smolcoin50)
            entries.addItem(smolcoin100)
        }
        .build()

    internal fun init() {
        Registries.ITEM {
            smolcoin1 withId SmolcoinsInit.id("smolcoin_1")
            smolcoin5 withId SmolcoinsInit.id("smolcoin_5")
            smolcoin10 withId SmolcoinsInit.id("smolcoin_10")
            smolcoin25 withId SmolcoinsInit.id("smolcoin_25")
            smolcoin50 withId SmolcoinsInit.id("smolcoin_50")
            smolcoin100 withId SmolcoinsInit.id("smolcoin_100")
            SmolcoinKeyItem withId SmolcoinsInit.id("key")
            exchangeBlockItem withId SmolcoinsInit.id("exchange")
            coinSlotBlockItem withId SmolcoinsInit.id("coin_slot")
        }
        Registries.ITEM_GROUP {
            itemGroup withId SmolcoinsInit.id("smolcoins")
        }
    }

}
