package org.ecorous.smolcoins.data

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import net.minecraft.resource.ResourceManager
import net.minecraft.util.Identifier
import org.ecorous.smolcoins.block.SmolcoinExchange.smolcoinConversions
import org.ecorous.smolcoins.init.SmolcoinsInit
import org.quiltmc.qsl.resource.loader.api.reloader.SimpleSynchronousResourceReloader

object SmolcoinsResourceReloader : SimpleSynchronousResourceReloader {
    @Serializable
    data class SmolcoinConversionJson(val replace: Boolean, val values: Map<String, Int>)

    @OptIn(ExperimentalSerializationApi::class)
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
                        smolcoinConversions.putAll(json.values.mapKeys { Identifier(it.key) })
                    }
                }
            } catch (e: Exception) {
                SmolcoinsInit.LOGGER.error("Error occurred while loading resource json $id", e)
            }
        }
    }

    override fun getQuiltId() = SmolcoinsInit.id("conversions")

}
