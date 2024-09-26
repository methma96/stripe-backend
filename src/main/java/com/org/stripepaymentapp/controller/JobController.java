package com.org.stripepaymentapp.controller;



import com.org.stripepaymentapp.dto.ServiceRequest;
import com.org.stripepaymentapp.model.Job;
import com.org.stripepaymentapp.model.Service;
import com.org.stripepaymentapp.repository.JobRepository;
import com.org.stripepaymentapp.service.StripeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/jobs")
public class JobController {

    @Autowired
    private StripeService stripeService;

    @Autowired
    private JobRepository jobRepository;


    @PostMapping("/{jobId}/approve")
    public String approveJobPayment(@PathVariable String jobId) throws Exception {
        Optional<Job> job = jobRepository.findById(jobId);
        if (job.isPresent()) {
            Job jobRequest = job.get();
            // Assuming payment has been made, transfer funds to the service provider
            stripeService.transferAmountToServiceProvider(jobRequest.getServiceProviderId(), jobRequest.getAmount());
            jobRequest.setStatus("SUCCESS");
            jobRepository.save(jobRequest);
            return "Payment approved and transferred";
        } else {
            throw new Exception("Job not found");
        }
    }

    @PostMapping("/{jobId}/cancel")
    public String cancelPayment(@PathVariable String jobId) throws Exception {
        Optional<Job> job = jobRepository.findById(jobId);
        if (job.isPresent()) {
            Job jobRequest = job.get();
            // Assuming payment has been made, transfer funds to the service provider
            stripeService.transferAmountToServiceProvider(jobRequest.getServiceProviderId(), jobRequest.getAmount());
            jobRequest.setStatus("SUCCESS");
            jobRepository.save(jobRequest);
            return "Payment approved and transferred";
        } else {
            throw new Exception("Job not found");
        }
    }

    @GetMapping("/")
    public ResponseEntity<List<Job>> getJobsForCustomer(@RequestParam long customerID) {
        try {
            List<Job> jobList = jobRepository.findByServiceId(customerID);
            return ResponseEntity.ok(jobList);
        } catch (Exception e) {
            // Log the error for debugging purposes
            System.err.println("Error fetching services: " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);

        }
    }



}
