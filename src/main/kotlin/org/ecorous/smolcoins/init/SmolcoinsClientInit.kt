package org.ecorous.smolcoins.init

import net.minecraft.client.gui.screen.ingame.HandledScreens
import org.ecorous.smolcoins.block.SmolcoinExchange.exchangeScreenHandlerType
import org.ecorous.smolcoins.block.SmolcoinExchangeScreen
import org.quiltmc.loader.api.ModContainer
import org.quiltmc.loader.api.minecraft.ClientOnly
import org.quiltmc.qsl.base.api.entrypoint.client.ClientModInitializer

@ClientOnly
object SmolcoinsClientInit : ClientModInitializer {
    override fun onInitializeClient(mod: ModContainer?) {
        HandledScreens.register(exchangeScreenHandlerType) { handler, playerInventory, _ ->
            SmolcoinExchangeScreen(handler, playerInventory)
        }
    }
}
