package com.example.ecommerce.payment.adapter.outbound.storage;

import com.example.ecommerce.payment.domain.port.outbound.ReceiptStoragePort;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.net.URI;

@Component
public class MinioReceiptStorageAdapter implements ReceiptStoragePort {

    private final S3Client s3;
    private final String bucket;

    public MinioReceiptStorageAdapter(
            @Value("${minio.endpoint}") String endpoint,
            @Value("${minio.access-key}") String accessKey,
            @Value("${minio.secret-key}") String secretKey,
            @Value("${minio.receipt-bucket:payment-receipts}") String bucket) {
        var creds = AwsBasicCredentials.create(accessKey, secretKey);
        this.s3 = S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .credentialsProvider(StaticCredentialsProvider.create(creds))
                .region(Region.US_EAST_1)
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .build();
        this.bucket = bucket;
    }

    @PreDestroy
    void close() {
        s3.close();
    }

    @Override
    public URI storeReceipt(String paymentId, byte[] pdfBytes) {
        ensureBucket();
        String key = "receipts/" + paymentId + ".pdf";
        s3.putObject(PutObjectRequest.builder()
                .bucket(bucket).key(key).contentType("application/pdf").contentLength((long) pdfBytes.length).build(),
                RequestBody.fromBytes(pdfBytes));
        return URI.create("s3://" + bucket + "/" + key);
    }

    private void ensureBucket() {
        try {
            s3.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
        } catch (NoSuchBucketException e) {
            s3.createBucket(b -> b.bucket(bucket));
        }
    }
}
