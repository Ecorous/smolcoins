package org.ecorous.smolcoins

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.client.gui.screen.ingame.HandledScreens
import net.minecraft.feature_flags.FeatureFlags
import net.minecraft.item.*
import net.minecraft.registry.Registries
import net.minecraft.resource.ResourceManager
import net.minecraft.resource.ResourceType
import net.minecraft.screen.ScreenHandlerType
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import org.quiltmc.loader.api.ModContainer
import org.quiltmc.loader.api.minecraft.ClientOnly
import org.quiltmc.qkl.library.registry.*
import org.quiltmc.qsl.base.api.entrypoint.ModInitializer
import org.quiltmc.qsl.base.api.entrypoint.client.ClientModInitializer
import org.quiltmc.qsl.block.entity.api.QuiltBlockEntityTypeBuilder
import org.quiltmc.qsl.item.setting.api.QuiltItemSettings
import org.quiltmc.qsl.resource.loader.api.ResourceLoader
import org.quiltmc.qsl.resource.loader.api.reloader.SimpleSynchronousResourceReloader
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
@Serializable
data class SmolcoinConversionJson(val replace: Boolean, val values: Map<String, Int>)
object Smolcoins : ModInitializer {
    var smolcoinConversions = mutableMapOf<Identifier, Int>()
    private fun id(id: String): Identifier {
        return Identifier("smolcoins", id)
    }

    private val SMOLCOIN_SETTINGS = QuiltItemSettings()
    val LOGGER: Logger = LoggerFactory.getLogger("smolcoins")

    val smolcoin_1: Item = Item(SMOLCOIN_SETTINGS)
    val smolcoin_5: Item = Item(SMOLCOIN_SETTINGS)
    val smolcoin_10: Item = Item(SMOLCOIN_SETTINGS)
    val smolcoin_25: Item = Item(SMOLCOIN_SETTINGS)
    val smolcoin_50: Item = Item(SMOLCOIN_SETTINGS)
    val smolcoin_100: Item = Item(SMOLCOIN_SETTINGS)

    val exchangeBlockItem = BlockItem(SmolcoinExchangeBlock, QuiltItemSettings())
    val exchangeBlockEntity: BlockEntityType<SmolcoinExchangeBlockEntity> = QuiltBlockEntityTypeBuilder.create({ pos, state -> SmolcoinExchangeBlockEntity(pos, state) }, SmolcoinExchangeBlock).build()
    val exchangeScreenHandlerType = ScreenHandlerType({syncId, playerInventory ->
        SmolcoinExchangeScreenHandler(syncId, playerInventory)
    }, FeatureFlags.DEFAULT_SET)

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
    @OptIn(ExperimentalSerializationApi::class)
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
        Registries.ITEM_GROUP {
            itemGroup withId id("smolcoins")
        }
        ResourceLoader.get(ResourceType.SERVER_DATA).registerReloader(object : SimpleSynchronousResourceReloader {
            override fun reload(manager: ResourceManager) {
                for((id, res) in manager.findResources("smolcoins") { path ->
                    path.path.endsWith("conversions.json")
                }) {
                    try {
                        res.open().use { stream ->
                            val json = Json.decodeFromStream<SmolcoinConversionJson>(stream)
                            if(json.replace) {
                                smolcoinConversions = json.values.mapKeys { Identifier(it.key) } as MutableMap<Identifier, Int>
                            } else {
                                smolcoinConversions.putAll(json.values.mapKeys {Identifier(it.key)})
                            }
                        }
                    } catch (e: Exception) {
                        LOGGER.error("Error occurred while loading resource json $id", e)
                    }
                }
            }

            override fun getQuiltId() = id("conversions")

        })
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
