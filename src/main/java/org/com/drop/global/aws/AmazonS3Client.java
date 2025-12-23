package org.com.drop.global.aws;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import org.apache.tika.Tika;
import org.com.drop.domain.user.entity.User;
import org.com.drop.global.exception.ErrorCode;
import org.com.drop.global.exception.ServiceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
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
@Slf4j
public class AmazonS3Client {
	private final S3Presigner s3Presigner;
	@Value("${spring.cloud.aws.s3.bucket}")
	private String bucket;
	private final S3Client s3Client;
	private final Tika tika = new Tika();

	public List<String> createPresignedUrls(List<PreSignedUrlRequest> requests, User actor) {
		return requests.stream()
			.map(req -> generateSinglePresignedUrl(req, actor))
			.toList();
	}

	private String generateSinglePresignedUrl(PreSignedUrlRequest req, User actor) {
		String path = generateFileName(req.contentType(), actor);

		PutObjectRequest putObjectRequest = PutObjectRequest.builder()
			.bucket(bucket)
			.key(path)
			.contentType(req.contentType())
			.contentLength(req.contentLength())
			.tagging("status=pending")
			.build();

		PutObjectPresignRequest preSignRequest = PutObjectPresignRequest.builder()
			.signatureDuration(Duration.ofMinutes(5))
			.putObjectRequest(putObjectRequest)
			.build();

		String url = s3Presigner.presignPutObject(preSignRequest).url().toString();

		return url;
	}

	private String generateFileName(String contentType, User actor) {
		String uuid = UUID.randomUUID().toString();
		String extension = switch (contentType) {
			case "image/jpeg" -> ".jpg";
			case "image/png"  -> ".png";
			case "image/webp" -> ".webp";
			case "image/gif"  -> ".gif";
			default -> throw new ServiceException(ErrorCode.INVALID_IMAGE_TYPE, "지원하지 않는 형식입니다: " + contentType);
		};
		return actor.getId() + uuid + extension;
	}

	public void verifyImage(List<String> keys) {
		for (String key : keys) {
			try {
				GetObjectRequest getObjectRequest =
					GetObjectRequest.builder().bucket(bucket).key(key).range("bytes=0-1024").build();

				ResponseInputStream<GetObjectResponse> is = s3Client.getObject(getObjectRequest);
				try {
					String contentType = tika.detect(is);
					Long contentLength = is.response().contentLength();

					if (contentLength > 10 * 1024 * 1024) {
						updateS3Tag(key, "deleted");
						throw new ServiceException(
							ErrorCode.INVALID_IMAGE_SIZE, ErrorCode.INVALID_IMAGE_SIZE.getMessage());
					}
					if (contentType == null || !contentType.startsWith("image/")) {
						updateS3Tag(key, "deleted");
						throw new ServiceException(
							ErrorCode.INVALID_IMAGE_TYPE, ErrorCode.INVALID_IMAGE_TYPE.getMessage());
					}
				} catch (IOException e) {
					throw new ServiceException(ErrorCode.VALIDATION_ERROR, "파일 분석 중 오류 발생: " + key);
				}
			} catch (Exception e) {
				throw new ServiceException(ErrorCode.INVALID_IMAGE, "이미지를 찾을 수 없습니다 : " + key);
			}
		}
		for (String key : keys) {
			updateS3Tag(key, "valid");
		}
	}

	public void updateS3Tag(String key, String status) {
		Tagging newTagging = Tagging.builder()
			.tagSet(Tag.builder().key("status").value(status).build())
			.build();

		PutObjectTaggingRequest taggingRequest = PutObjectTaggingRequest.builder()
			.bucket(bucket).key(key).tagging(newTagging).build();

		s3Client.putObjectTagging(taggingRequest);
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
