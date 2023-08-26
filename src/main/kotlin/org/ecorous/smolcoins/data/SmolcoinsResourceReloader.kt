package org.ecorous.smolcoins.data

import kotlinx.serialization.Serializable
import net.minecraft.resource.ResourceManager
import org.ecorous.smolcoins.init.SmolcoinsInit
import org.quiltmc.qsl.resource.loader.api.reloader.SimpleSynchronousResourceReloader

object SmolcoinsResourceReloader : SimpleSynchronousResourceReloader {
    @Serializable
    data class SmolcoinConversionJson(val replace: Boolean, val values: Map<String, Int>)

    override fun reload(manager: ResourceManager) {
        SmolcoinConversions.reload(manager)
    }

    override fun getQuiltId() = SmolcoinsInit.id("conversions")

}
