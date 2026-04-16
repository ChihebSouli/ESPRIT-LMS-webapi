package com.esprit.lms.shared.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.SetBucketPolicyArgs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class MinioConfig {

    @Value("${minio.endpoint}")
    private String endpoint;

    @Value("${minio.access-key}")
    private String accessKey;

    @Value("${minio.secret-key}")
    private String secretKey;

    @Value("${minio.bucket.documents}")
    private String documentsBucket;

    @Value("${minio.bucket.covers}")
    private String coversBucket;

    @Bean
    public MinioClient minioClient() {
        MinioClient client = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();

        // Ensure buckets exist on startup
        try {
            ensureBucket(client, documentsBucket, false);
            ensureBucket(client, coversBucket, true);
            log.info("MinIO connected at {} — buckets ready", endpoint);
        } catch (Exception e) {
            log.warn("MinIO not reachable at startup ({}). Will retry on first use.", e.getMessage());
        }

        return client;
    }

    private void ensureBucket(MinioClient client, String bucket, boolean publicRead) throws Exception {
        if (!client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
            client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            log.info("Created MinIO bucket: {}", bucket);

            if (publicRead) {
                // Set public read policy for covers bucket
                String policy = """
                    {
                        "Version": "2012-10-17",
                        "Statement": [{
                            "Effect": "Allow",
                            "Principal": {"AWS": ["*"]},
                            "Action": ["s3:GetObject"],
                            "Resource": ["arn:aws:s3:::%s/*"]
                        }]
                    }
                    """.formatted(bucket);
                client.setBucketPolicy(SetBucketPolicyArgs.builder()
                        .bucket(bucket)
                        .config(policy)
                        .build());
                log.info("Set public read policy on bucket: {}", bucket);
            }
        }
    }
}
