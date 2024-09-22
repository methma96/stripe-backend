package com.org.stripepaymentapp.dto;

import java.util.List;

public class PaymentLinkRequest {
    private List<String> lineItems; // List of product IDs, or item details
    private Long amount; // Amount in cents
    private String currency;

    private int quantity;

    // Getters and setters
    public List<String> getLineItems() {
        return lineItems;
    }

    public void setLineItems(List<String> lineItems) {
        this.lineItems = lineItems;
    }

    public Long getAmount() {
        return amount;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }
}
