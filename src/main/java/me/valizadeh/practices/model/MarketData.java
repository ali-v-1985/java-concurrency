package me.valizadeh.practices.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Immutable MarketData record for thread safety
 */
public record MarketData(
        String symbol,
        BigDecimal bid,
        BigDecimal ask,
        BigDecimal lastPrice,
        long volume,
        LocalDateTime timestamp
) {
    public MarketData {
        if (timestamp == null) timestamp = LocalDateTime.now();
    }
    
    public static MarketData of(String symbol, BigDecimal bid, BigDecimal ask, BigDecimal lastPrice, long volume) {
        return new MarketData(symbol, bid, ask, lastPrice, volume, null);
    }
}
