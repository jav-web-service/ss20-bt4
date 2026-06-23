package com.ss20bt4.client;

import com.ss20bt4.dto.CertificateRequest;
import com.ss20bt4.dto.CertificateResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

// MockAPI or similar external service URL
@FeignClient(name = "certificateClient", url = "https://mockapi.io/api/v1/certificates") // Using a dummy mockapi.io base URL
public interface CertificateClient {
    
    @PostMapping("/claim")
    CertificateResponse claimCertificate(@RequestBody CertificateRequest request);
}
