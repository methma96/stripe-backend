package com.org.stripepaymentapp.model;


import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "requested_jobs")
public class Job {

    @Id
    private String id;
//    private String priceId;
    private String serviceProviderId;



    private long customerId;
    private String description;
    private double amount;
    private String status; // PENDING, IN_PROGRESS, COMPLETED, DISPUTE
    private String paymentStatus; // ESCROW, CAPTURED, REFUNDED

    private String name;

    private String currency;

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(long customerId) {
        this.customerId = customerId;
    }


//    public String getPriceId() {
//        return priceId;
//    }
//
//    public void setPriceId(String priceId) {
//        this.priceId = priceId;
//    }

    public String getServiceProviderId() {
        return serviceProviderId;
    }

    public void setServiceProviderId(String serviceProviderId) {
        this.serviceProviderId = serviceProviderId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

}
