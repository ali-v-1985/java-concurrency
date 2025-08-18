package me.valizadeh.practices.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Immutable Trade record for thread safety
 */
public record Trade(
        String id,
        String symbol,
        BigDecimal price,
        int quantity,
        TradeType type,
        String clientId,
        LocalDateTime timestamp,
        TradeStatus status
) {
    public Trade {
        if (id == null) id = UUID.randomUUID().toString();
        if (timestamp == null) timestamp = LocalDateTime.now();
        if (status == null) status = TradeStatus.PENDING;
    }
    
    public static Trade buy(String symbol, BigDecimal price, int quantity, String clientId) {
        return new Trade(null, symbol, price, quantity, TradeType.BUY, clientId, null, null);
    }
    
    public static Trade sell(String symbol, BigDecimal price, int quantity, String clientId) {
        return new Trade(null, symbol, price, quantity, TradeType.SELL, clientId, null, null);
    }
    
    public Trade withStatus(TradeStatus newStatus) {
        return new Trade(id, symbol, price, quantity, type, clientId, timestamp, newStatus);
    }
}
