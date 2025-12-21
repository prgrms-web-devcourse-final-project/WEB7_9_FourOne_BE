package org.com.drop.global.aws;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Service
@RequiredArgsConstructor
public class AmazonS3Client {
	private final S3Presigner s3Presigner;
	@Value("${spring.cloud.aws.s3.bucket}")
	private String bucket;

	public String createPresignedUrl(String path) {

		var putObjectRequest = PutObjectRequest.builder()
			.bucket(bucket)
			.key(path)
			.build();

		var preSignRequest = PutObjectPresignRequest.builder()
			.signatureDuration(Duration.ofMinutes(3))
			.putObjectRequest(putObjectRequest)
			.build();

		return s3Presigner.presignPutObject(preSignRequest).url().toString();
	}

	public String getPresignedUrl(String key) {
		GetObjectRequest objectRequest = GetObjectRequest.builder()
			.bucket(bucket)
			.key(key)
			.build();

		GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
			.signatureDuration(Duration.ofMinutes(5))
			.getObjectRequest(objectRequest)
			.build();

		return s3Presigner.presignGetObject(presignRequest).url().toString();
	}
}
