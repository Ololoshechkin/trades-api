package com.example.trades

import com.example.trades.px.TradesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import java.time.Instant

private data class TimestampDescriptor(val timestamp: Instant, val relativeIndex: Int = 0)

fun trades(powerExchange: TradesApi, since: Instant): Flow<Trade> = channelFlow {
        val historicalChanel = Channel<Trade>(capacity = 100)
        val needMoreChanel = Channel<TimestampDescriptor>()

        launch {
            needMoreChanel.send(TimestampDescriptor(since))
        }

        // Historical sequence:
        launch {
            while (true) {
                needMoreChanel.receive().let { nextDescriptor ->
                    var tradesWithTimestamp = 0
                    powerExchange.query(nextDescriptor.timestamp).forEach {
                        if (it.timestamp == nextDescriptor.timestamp) {
                            tradesWithTimestamp++
                        }
                        if (tradesWithTimestamp >= nextDescriptor.relativeIndex) {
                            historicalChanel.send(it)
                        }
                    }
                }
            }
        }

        // Real-time sequence:
        launch {
            var lastTimestamp = since
            var relativeIndex = 0

            while (true) {
                try {
                    var isUpToDate = false
                    powerExchange.realtimeUpdates().forEach { nextTrade ->
                        while (!isUpToDate) {
                            for (historicalTrade in historicalChanel) {
                                if (nextTrade.tradeId == historicalTrade.tradeId) {
                                    isUpToDate = true
                                    continue
                                }

                                send(historicalTrade)
                                if (historicalTrade.timestamp == lastTimestamp) {
                                    relativeIndex++
                                } else {
                                    relativeIndex = 1
                                }
                                lastTimestamp = historicalTrade.timestamp
                            }
                            if (!isUpToDate) {
                                needMoreChanel.send(TimestampDescriptor(lastTimestamp, relativeIndex))
                            }
                        }

                        send(nextTrade)
                        if (nextTrade.timestamp == lastTimestamp) {
                            relativeIndex++
                        } else {
                            relativeIndex = 1
                        }
                        lastTimestamp = nextTrade.timestamp
                    }
                } catch (e: Throwable) {
                    needMoreChanel.send(TimestampDescriptor(lastTimestamp, relativeIndex))
                }
            }
        }
    }