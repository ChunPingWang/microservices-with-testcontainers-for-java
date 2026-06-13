package com.example.ecommerce.shared.port;

import java.io.InputStream;
import java.net.URI;
import java.time.Duration;

/**
 * Outbound port for storing & retrieving binary objects.
 * Real adapter: MinIO / S3. Test adapter: in-memory.
 */
public interface ObjectStoragePort {

    URI store(String bucket, String key, InputStream content, long contentLength, String contentType);

    InputStream retrieve(String bucket, String key);

    /** Pre-signed URL with expiry; not all adapters need to support this. */
    URI presignedGetUrl(String bucket, String key, Duration ttl);
}
