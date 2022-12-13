package com.example.trades

import java.math.BigDecimal
import java.time.Instant

enum class Side {
    Sell, Buy
}

data class Trade(
    val tradeId: String,
    val timestamp: Instant,
    val price: BigDecimal,
    val volume: BigDecimal,
    val side: Side
)