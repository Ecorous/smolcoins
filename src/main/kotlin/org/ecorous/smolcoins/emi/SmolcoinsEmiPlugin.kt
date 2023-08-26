package org.ecorous.smolcoins.emi

import dev.emi.emi.api.EmiPlugin
import dev.emi.emi.api.EmiRegistry
import org.ecorous.smolcoins.block.SmolcoinExchange
import org.quiltmc.loader.api.minecraft.ClientOnly

@ClientOnly
object SmolcoinsEmiPlugin : EmiPlugin {
    override fun register(registry: EmiRegistry) {
        SmolcoinExchange.initEmi(registry)
    }
}
