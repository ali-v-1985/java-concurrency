package me.valizadeh.practices.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Portfolio model for tracking positions and cash
 * Uses standard SLF4J logging and explicit getters/synchronization
 */
public class Portfolio {
    private static final Logger logger = LoggerFactory.getLogger(Portfolio.class);
    
    private final String clientId;
    private final Map<String, Integer> positions = new ConcurrentHashMap<>();
    private BigDecimal cash;
    
    public Portfolio(String clientId, BigDecimal initialCash) {
        this.clientId = clientId;
        this.cash = initialCash;
        logger.debug("Created portfolio for client {} with initial cash {}", clientId, initialCash);
    }
    
    public String getClientId() {
        return clientId;
    }
    
    public synchronized BigDecimal getCash() {
        return cash;
    }
    
    public synchronized void setCash(BigDecimal cash) {
        this.cash = cash;
    }
    
    public Map<String, Integer> getPositions() {
        return new ConcurrentHashMap<>(positions);
    }
    
    public synchronized int getPosition(String symbol) {
        return positions.getOrDefault(symbol, 0);
    }
    
    public synchronized void updatePosition(String symbol, int quantity) {
        positions.merge(symbol, quantity, Integer::sum);
        if (positions.get(symbol) == 0) {
            positions.remove(symbol);
        }
        logger.debug("Updated position for {}: {} (change: {})", symbol, positions.get(symbol), quantity);
    }
    
    @Override
    public synchronized String toString() {
        return String.format("Portfolio[client=%s, cash=%s, positions=%s]", 
            clientId, cash, positions);
    }
}
