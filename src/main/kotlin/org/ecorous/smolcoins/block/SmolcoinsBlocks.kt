package org.ecorous.smolcoins.block

object SmolcoinsBlocks {
    internal fun init() {
        SmolcoinExchange.init()
        CoinSlot.init()
    }
}
