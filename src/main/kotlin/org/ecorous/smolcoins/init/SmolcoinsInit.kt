package org.ecorous.smolcoins.init

import net.minecraft.item.ItemStack
import net.minecraft.resource.ResourceType
import net.minecraft.util.Identifier
import org.ecorous.smolcoins.SmolcoinsItems

import org.ecorous.smolcoins.block.SmolcoinsBlocks
import org.ecorous.smolcoins.data.SmolcoinsResourceReloader
import org.quiltmc.loader.api.ModContainer
import org.quiltmc.qsl.base.api.entrypoint.ModInitializer
import org.quiltmc.qsl.resource.loader.api.ResourceLoader
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object SmolcoinsInit : ModInitializer {
    internal fun id(id: String): Identifier {
        return Identifier("smolcoins", id)
    }

    val LOGGER: Logger = LoggerFactory.getLogger("smolcoins")

    override fun onInitialize(mod: ModContainer) {
        ResourceLoader.get(ResourceType.SERVER_DATA).registerReloader(SmolcoinsResourceReloader)
        LOGGER.info("Hello Quilt world from {}!", mod.metadata()?.name())
        SmolcoinsBlocks.init()
        SmolcoinsItems.init()
    }


}
