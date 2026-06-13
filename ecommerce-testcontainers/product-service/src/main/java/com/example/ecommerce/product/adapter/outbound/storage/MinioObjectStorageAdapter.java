package com.example.ecommerce.product.adapter.outbound.storage;

import com.example.ecommerce.shared.port.ObjectStoragePort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import jakarta.annotation.PreDestroy;

import java.io.InputStream;
import java.net.URI;
import java.time.Duration;

/**
 * Object storage backed by an S3-compatible service (MinIO). Uses path-style
 * access because MinIO does not support virtual-host style for arbitrary
 * bucket names.
 */
@Component
public class MinioObjectStorageAdapter implements ObjectStoragePort {

    private final S3Client s3;
    private final S3Presigner presigner;

    public MinioObjectStorageAdapter(
            @Value("${minio.endpoint}") String endpoint,
            @Value("${minio.access-key}") String accessKey,
            @Value("${minio.secret-key}") String secretKey) {
        var creds = AwsBasicCredentials.create(accessKey, secretKey);
        this.s3 = S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .credentialsProvider(StaticCredentialsProvider.create(creds))
                .region(Region.US_EAST_1)
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .build();
        this.presigner = S3Presigner.builder()
                .endpointOverride(URI.create(endpoint))
                .credentialsProvider(StaticCredentialsProvider.create(creds))
                .region(Region.US_EAST_1)
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .build();
    }

    @PreDestroy
    void close() {
        s3.close();
        presigner.close();
    }

    @Override
    public URI store(String bucket, String key, InputStream content, long contentLength, String contentType) {
        ensureBucket(bucket);
        s3.putObject(PutObjectRequest.builder()
                        .bucket(bucket).key(key).contentType(contentType).contentLength(contentLength).build(),
                RequestBody.fromInputStream(content, contentLength));
        return URI.create("s3://" + bucket + "/" + key);
    }

    @Override
    public InputStream retrieve(String bucket, String key) {
        return s3.getObject(GetObjectRequest.builder().bucket(bucket).key(key).build());
    }

    @Override
    public URI presignedGetUrl(String bucket, String key, Duration ttl) {
        var presigned = presigner.presignGetObject(GetObjectPresignRequest.builder()
                .signatureDuration(ttl)
                .getObjectRequest(GetObjectRequest.builder().bucket(bucket).key(key).build())
                .build());
        return presigned.url().toString().transform(URI::create);
    }

    private void ensureBucket(String bucket) {
        try {
            s3.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
        } catch (NoSuchBucketException ex) {
            s3.createBucket(b -> b.bucket(bucket));
        }
    }
}
