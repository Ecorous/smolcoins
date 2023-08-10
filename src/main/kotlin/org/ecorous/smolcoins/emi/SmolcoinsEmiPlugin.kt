package org.ecorous.smolcoins.emi

import dev.emi.emi.api.EmiPlugin
import dev.emi.emi.api.EmiRegistry
import dev.emi.emi.api.recipe.EmiRecipeCategory
import dev.emi.emi.api.render.EmiTexture
import dev.emi.emi.api.stack.EmiStack
import net.minecraft.util.Identifier
import org.ecorous.smolcoins.SmolcoinsItems
import org.ecorous.smolcoins.block.SmolcoinExchange
import org.ecorous.smolcoins.init.SmolcoinsInit
import org.quiltmc.loader.api.minecraft.ClientOnly

@ClientOnly
object SmolcoinsEmiPlugin : EmiPlugin {
    override fun register(registry: EmiRegistry) {
        SmolcoinExchange.initEmi(registry)
    }
}
