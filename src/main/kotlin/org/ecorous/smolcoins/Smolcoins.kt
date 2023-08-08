package org.ecorous.smolcoins

import net.minecraft.client.gui.screen.ingame.HandledScreens
import net.minecraft.feature_flags.FeatureFlags
import net.minecraft.inventory.Inventory
import net.minecraft.item.BlockItem
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.registry.Registries
import net.minecraft.screen.ScreenHandlerType
import net.minecraft.util.Identifier
import net.minecraft.util.collection.DefaultedList
import org.quiltmc.loader.api.ModContainer
import org.quiltmc.loader.api.minecraft.ClientOnly
import org.quiltmc.qsl.base.api.entrypoint.ModInitializer
import org.quiltmc.qsl.item.setting.api.QuiltItemSettings
import org.quiltmc.qkl.library.registry.*
import org.quiltmc.qsl.base.api.entrypoint.client.ClientModInitializer
import org.quiltmc.qsl.block.entity.api.QuiltBlockEntityTypeBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@ClientOnly
object SmolcoinsClient : ClientModInitializer {
    override fun onInitializeClient(mod: ModContainer?) {
        HandledScreens.register(Smolcoins.exchangeScreenHandlerType) { handler, playerInventory, _ ->
            SmolcoinExchangeScreen(handler, playerInventory)
        }
    }
}

object Smolcoins : ModInitializer {
    private fun id(id: String): Identifier {
        return Identifier("smolcoins", id)
    }

    val SMOLCOIN_CONVERSION = mapOf(
        Items.COPPER_BLOCK to 5,
        Items.COAL to 1,
        Items.DIAMOND to 45,
        Items.IRON_INGOT to 5,
        Items.GOLD_INGOT to 5,
        Items.NETHERITE_INGOT to 100,
    )
    private val SMOLCOIN_SETTINGS = QuiltItemSettings()
    val LOGGER: Logger = LoggerFactory.getLogger("smolcoins")

    val smolcoin_1: Item = Item(SMOLCOIN_SETTINGS)
    val smolcoin_5: Item = Item(SMOLCOIN_SETTINGS)
    val smolcoin_10: Item = Item(SMOLCOIN_SETTINGS)
    val smolcoin_25: Item = Item(SMOLCOIN_SETTINGS)
    val smolcoin_50: Item = Item(SMOLCOIN_SETTINGS)
    val smolcoin_100: Item = Item(SMOLCOIN_SETTINGS)

    val exchangeBlockItem = BlockItem(SmolcoinExchangeBlock, QuiltItemSettings())
    val exchangeBlockEntity = QuiltBlockEntityTypeBuilder.create({ pos, state -> SmolcoinExchangeBlockEntity(pos, state) }, SmolcoinExchangeBlock).build()
    val exchangeScreenHandlerType = ScreenHandlerType({syncId, playerInventory ->
        SmolcoinExchangeScreenHandler(syncId, playerInventory)
    }, FeatureFlags.DEFAULT_SET)

    override fun onInitialize(mod: ModContainer) {
        Registries.ITEM {
            smolcoin_1 withId id("smolcoin_1")
            smolcoin_5 withId id("smolcoin_5")
            smolcoin_10 withId id("smolcoin_10")
            smolcoin_25 withId id("smolcoin_25")
            smolcoin_50 withId id("smolcoin_50")
            smolcoin_100 withId id("smolcoin_100")
            exchangeBlockItem withId id("exchange")
        }
        Registries.BLOCK {
            SmolcoinExchangeBlock withId id("exchange")
        }
        Registries.BLOCK_ENTITY_TYPE {
            exchangeBlockEntity withId id("exchange")
        }
        Registries.SCREEN_HANDLER_TYPE {
            exchangeScreenHandlerType withId id("exchange")
        }
        LOGGER.info("Hello Quilt world from {}!", mod.metadata()?.name())
    }

    fun smolcoinsToItems(smolcoins: Int): Array<ItemStack> {
        val items = mutableListOf<ItemStack>()

        var remainingSmolcoins = smolcoins

        val smolcoins100 = remainingSmolcoins / 100
        items.add(ItemStack(smolcoin_100, smolcoins100))
        remainingSmolcoins -= smolcoins100 * 100

        val smolcoins50 = remainingSmolcoins / 50
        items.add(ItemStack(smolcoin_50, smolcoins50))
        remainingSmolcoins -= smolcoins50 * 50

        val smolcoins25 = remainingSmolcoins / 25
        items.add(ItemStack(smolcoin_25, smolcoins25))
        remainingSmolcoins -= smolcoins25 * 25

        val smolcoins10 = remainingSmolcoins / 10
        items.add(ItemStack(smolcoin_10, smolcoins10))
        remainingSmolcoins -= smolcoins10 * 10

        val smolcoins5 = remainingSmolcoins / 5
        items.add(ItemStack(smolcoin_5, smolcoins5))
        remainingSmolcoins -= smolcoins5 * 5

        items.add(ItemStack(smolcoin_1, remainingSmolcoins))

        return items.groupBy { it.item }.map { (_, itemStacks) ->
            itemStacks.reduce { acc, itemStack -> acc.also { it.count += itemStack.count } }
        }.toTypedArray()
    }
}
