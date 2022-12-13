package com.example.trades.px

import com.example.trades.Trade
import java.time.Instant

interface TradesApi {
    fun realtimeUpdates(): Iterable<Trade>
    fun query(since: Instant): List<Trade>
}