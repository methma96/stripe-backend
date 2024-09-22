package com.org.stripepaymentapp.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "payments")
public class Payment {
    @Id
    private String id;
    private String requestedJobId;
    private double amount;

    private String status; // HELD, CAPTURED, REFUNDED
    private String stripeTransferId; // For payout tracking

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRequestedJobId() {
        return requestedJobId;
    }

    public void setRequestedJobId(String requestedJobId) {
        this.requestedJobId = requestedJobId;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStripeTransferId() {
        return stripeTransferId;
    }

    public void setStripeTransferId(String stripeTransferId) {
        this.stripeTransferId = stripeTransferId;
    }


}