package faiss.manager.core.storage;

import faiss.manager.common.ErrorCode;
import faiss.manager.common.exception.FaissManagerException;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.util.ArrayList;
import java.util.List;

/**
 * S3 索引文件存储实现
 */
@Slf4j
public class S3IndexRepository implements IndexRepository {

    private static final String INDEX_FILE_SUFFIX = ".faiss";

    private final S3Client s3Client;
    private final String bucket;
    private final String prefix;

    public S3IndexRepository(S3Client s3Client, String bucket, String prefix) {
        this.s3Client = s3Client;
        this.bucket = bucket;
        this.prefix = normalizePrefix(prefix);
    }

    @Override
    public void save(String indexId, byte[] data) {
        String key = resolveKey(indexId);
        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .contentType("application/octet-stream")
                            .build(),
                    RequestBody.fromBytes(data)
            );
            log.info("Saved index to S3: s3://{}/{}, size={} bytes", bucket, key, data.length);
        } catch (Exception e) {
            throw new FaissManagerException(ErrorCode.STORAGE_WRITE_FAILED,
                    "S3 写入失败: " + e.getMessage(), e);
        }
    }

    @Override
    public byte[] load(String indexId) {
        String key = resolveKey(indexId);
        try {
            byte[] data = s3Client.getObject(
                    GetObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .build(),
                    ResponseTransformer.toBytes()
            ).asByteArray();
            log.info("Loaded index from S3: s3://{}/{}, size={} bytes", bucket, key, data.length);
            return data;
        } catch (NoSuchKeyException e) {
            throw new FaissManagerException(ErrorCode.INDEX_NOT_FOUND,
                    "S3 索引文件不存在: s3://" + bucket + "/" + key);
        } catch (Exception e) {
            throw new FaissManagerException(ErrorCode.STORAGE_READ_FAILED,
                    "S3 读取失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(String indexId) {
        String key = resolveKey(indexId);
        try {
            s3Client.deleteObject(
                    DeleteObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .build()
            );
            log.info("Deleted index from S3: s3://{}/{}", bucket, key);
        } catch (Exception e) {
            throw new FaissManagerException(ErrorCode.STORAGE_DELETE_FAILED,
                    "S3 删除失败: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean exists(String indexId) {
        String key = resolveKey(indexId);
        try {
            s3Client.headObject(
                    HeadObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .build()
            );
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (Exception e) {
            log.warn("Failed to check S3 object existence: s3://{}/{}", bucket, key, e);
            return false;
        }
    }

    @Override
    public List<String> listAll() {
        List<String> indexIds = new ArrayList<>();
        try {
            ListObjectsV2Request request = ListObjectsV2Request.builder()
                    .bucket(bucket)
                    .prefix(prefix)
                    .build();

            ListObjectsV2Response response;
            do {
                response = s3Client.listObjectsV2(request);
                for (S3Object obj : response.contents()) {
                    String key = obj.key();
                    if (key.endsWith(INDEX_FILE_SUFFIX)) {
                        String name = key.substring(prefix.length(),
                                key.length() - INDEX_FILE_SUFFIX.length());
                        indexIds.add(name);
                    }
                }
                request = request.toBuilder()
                        .continuationToken(response.nextContinuationToken())
                        .build();
            } while (Boolean.TRUE.equals(response.isTruncated()));
        } catch (Exception e) {
            log.warn("Failed to list S3 index files: s3://{}/{}", bucket, prefix, e);
        }
        return indexIds;
    }

    private String resolveKey(String indexId) {
        return prefix + indexId + INDEX_FILE_SUFFIX;
    }

    private String normalizePrefix(String prefix) {
        if (prefix == null || prefix.isEmpty()) {
            return "";
        }
        return prefix.endsWith("/") ? prefix : prefix + "/";
    }
}
