package me.valizadeh.practices.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * Immutable trading result representing the outcome of a trade execution
 */
public class TradingResult {
    private final String tradeId;
    private final TradeStatus status;
    private final BigDecimal executedPrice;
    private final int executedQuantity;
    private final Instant executionTime;
    private final String executionVenue;
    private final String errorMessage;
    
    // Private constructor for builder
    private TradingResult(Builder builder) {
        this.tradeId = builder.tradeId;
        this.status = builder.status;
        this.executedPrice = builder.executedPrice;
        this.executedQuantity = builder.executedQuantity;
        this.executionTime = builder.executionTime;
        this.executionVenue = builder.executionVenue;
        this.errorMessage = builder.errorMessage;
    }
    
    // Getters
    public String getTradeId() { return tradeId; }
    public TradeStatus getStatus() { return status; }
    public BigDecimal getExecutedPrice() { return executedPrice; }
    public int getExecutedQuantity() { return executedQuantity; }
    public Instant getExecutionTime() { return executionTime; }
    public String getExecutionVenue() { return executionVenue; }
    public String getErrorMessage() { return errorMessage; }
    
    /**
     * Factory method for successful trades
     */
    public static TradingResult success(String tradeId, BigDecimal price, int quantity, String venue) {
        return new Builder()
            .tradeId(tradeId)
            .status(TradeStatus.EXECUTED)
            .executedPrice(price)
            .executedQuantity(quantity)
            .executionTime(Instant.now())
            .executionVenue(venue)
            .build();
    }
    
    /**
     * Factory method for failed trades
     */
    public static TradingResult failure(String tradeId, String errorMessage) {
        return new Builder()
            .tradeId(tradeId)
            .status(TradeStatus.FAILED)
            .errorMessage(errorMessage)
            .executionTime(Instant.now())
            .build();
    }
    
    /**
     * Check if this result represents a successful trade
     */
    public boolean isSuccessful() {
        return status == TradeStatus.EXECUTED;
    }
    
    /**
     * Check if this result represents a failed trade
     */
    public boolean isFailed() {
        return status == TradeStatus.FAILED;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TradingResult that = (TradingResult) o;
        return executedQuantity == that.executedQuantity &&
                Objects.equals(tradeId, that.tradeId) &&
                status == that.status &&
                Objects.equals(executedPrice, that.executedPrice) &&
                Objects.equals(executionTime, that.executionTime) &&
                Objects.equals(executionVenue, that.executionVenue) &&
                Objects.equals(errorMessage, that.errorMessage);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(tradeId, status, executedPrice, executedQuantity, 
                           executionTime, executionVenue, errorMessage);
    }
    
    @Override
    public String toString() {
        return String.format("TradingResult[tradeId=%s, status=%s, price=%s, quantity=%d, time=%s, venue=%s, error=%s]",
                tradeId, status, executedPrice, executedQuantity, executionTime, executionVenue, errorMessage);
    }
    
    // Builder pattern
    public static class Builder {
        private String tradeId;
        private TradeStatus status;
        private BigDecimal executedPrice;
        private int executedQuantity;
        private Instant executionTime;
        private String executionVenue;
        private String errorMessage;
        
        public Builder tradeId(String tradeId) {
            this.tradeId = tradeId;
            return this;
        }
        
        public Builder status(TradeStatus status) {
            this.status = status;
            return this;
        }
        
        public Builder executedPrice(BigDecimal executedPrice) {
            this.executedPrice = executedPrice;
            return this;
        }
        
        public Builder executedQuantity(int executedQuantity) {
            this.executedQuantity = executedQuantity;
            return this;
        }
        
        public Builder executionTime(Instant executionTime) {
            this.executionTime = executionTime;
            return this;
        }
        
        public Builder executionVenue(String executionVenue) {
            this.executionVenue = executionVenue;
            return this;
        }
        
        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }
        
        public TradingResult build() {
            return new TradingResult(this);
        }
    }
}