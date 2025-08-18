package me.valizadeh.practices.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents an entry in the order book
 */
public record OrderBookEntry(
        String orderId,
        String symbol,
        TradeType side,
        BigDecimal price,
        int quantity,
        String clientId,
        LocalDateTime timestamp
) {
    public OrderBookEntry {
        if (timestamp == null) timestamp = LocalDateTime.now();
    }
    
    public static OrderBookEntry bid(String symbol, BigDecimal price, int quantity, String clientId) {
        return new OrderBookEntry(null, symbol, TradeType.BUY, price, quantity, clientId, null);
    }
    
    public static OrderBookEntry ask(String symbol, BigDecimal price, int quantity, String clientId) {
        return new OrderBookEntry(null, symbol, TradeType.SELL, price, quantity, clientId, null);
    }
}
