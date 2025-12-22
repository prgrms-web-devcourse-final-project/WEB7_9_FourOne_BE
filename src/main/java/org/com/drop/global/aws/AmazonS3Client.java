package org.com.drop.global.aws;

import java.time.Duration;

import org.apache.tika.Tika;
import org.com.drop.global.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectTaggingRequest;
import software.amazon.awssdk.services.s3.model.Tag;
import software.amazon.awssdk.services.s3.model.Tagging;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Service
@RequiredArgsConstructor
public class AmazonS3Client {
	private final S3Presigner s3Presigner;
	@Value("${spring.cloud.aws.s3.bucket}")
	private String bucket;
	private final S3Client s3Client;
	private final Tika tika = new Tika();

	public String createPresignedUrl(String path, PreSignedUrlRequest preSignedUrlRequest) {
		if (preSignedUrlRequest.contentLength() > 10 * 1024 * 1024) {
			throw new IllegalArgumentException("파일 크기는 10MB를 초과할 수 없습니다.");
		}
		if (!preSignedUrlRequest.contentType().startsWith("image/")) {
			throw new IllegalArgumentException("이미지 파일만 업로드 가능합니다.");
		}
		String tagging = "status=pending";

		var putObjectRequest = PutObjectRequest.builder()
			.bucket(bucket)
			.key(path)
			.contentType(preSignedUrlRequest.contentType())
			.contentLength(preSignedUrlRequest.contentLength())
			.tagging(tagging)
			.build();

		var preSignRequest = PutObjectPresignRequest.builder()
			.signatureDuration(Duration.ofMinutes(3))
			.putObjectRequest(putObjectRequest)
			.build();

		return s3Presigner.presignPutObject(preSignRequest).url().toString();
	}

	@Async
	public void verifyImageAsync( String key) {
		try {
			GetObjectRequest getObjectRequest = GetObjectRequest.builder()
				.bucket(bucket)
				.key(key)
				.range("bytes=0-1024")
				.build();

			ResponseInputStream<GetObjectResponse> is = s3Client.getObject(getObjectRequest);
			String actualMimeType = tika.detect(is);

			if (actualMimeType == null || !actualMimeType.startsWith("image/")) {
				deleteFile(bucket, key);
			}
			Tagging newTagging = Tagging.builder()
				.tagSet(Tag.builder().key("status").value("valid").build())
				.build();
			PutObjectTaggingRequest taggingRequest = PutObjectTaggingRequest.builder()
				.bucket(bucket)
				.key(key)
				.tagging(newTagging)
				.build();

			s3Client.putObjectTagging(taggingRequest);
		} catch (Exception e) {
			deleteFile(bucket, key);
		}
	}

	private void deleteFile(String bucket, String key) {
		s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
		throw ErrorCode.INVALID_IMAGE_UPLOAD.serviceException("잘못된 이미지 업로드 :" + key);
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
