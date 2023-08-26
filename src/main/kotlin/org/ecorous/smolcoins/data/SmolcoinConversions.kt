package org.ecorous.smolcoins.data

import dev.emi.emi.api.EmiRegistry
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import net.minecraft.item.ItemStack
import net.minecraft.resource.ResourceManager
import org.ecorous.smolcoins.block.SmolcoinExchangeEmiRecipe
import org.ecorous.smolcoins.init.SmolcoinsInit
import org.quiltmc.loader.api.minecraft.ClientOnly

object SmolcoinConversions {
    private var conversions: MutableMap<StackOrTag, Int> = hashMapOf()
    @OptIn(ExperimentalSerializationApi::class)
    fun reload(manager: ResourceManager) {
        for((id, res) in manager.findResources("smolcoins") { path ->
            path.path.endsWith("conversions.json")
        }) {
            try {
                res.open().use { stream ->
                    val json = Json.decodeFromStream<SmolcoinsResourceReloader.SmolcoinConversionJson>(stream)
                    val conversions = json.values.mapKeys { (k, _) -> StackOrTag.of(k) } as MutableMap<StackOrTag, Int>
                    if(json.replace) {
                        this.conversions = conversions
                    } else {
                        this.conversions.putAll(conversions)
                    }
                    for(conv in conversions) {
                        SmolcoinsInit.LOGGER.info("${conv.key}: ${conv.value}")
                    }
                }
            } catch (e: Exception) {
                SmolcoinsInit.LOGGER.error("Error occurred while loading resource json $id", e)
            }
        }
    }

    operator fun get(stack: ItemStack): List<Int> {
        for((k, v) in conversions) {
            val take = k.take(stack)
            if(take == 0) continue
            return listOf(take, v)
        }
        return listOf(0, 0)
    }
    @ClientOnly
    fun getEmiRecipes(registry: EmiRegistry) {
        for((k, v) in conversions) {
            registry.addRecipe(SmolcoinExchangeEmiRecipe(k, v))
        }
    }
}
