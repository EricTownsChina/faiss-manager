package faiss.manager.core.storage;

import faiss.manager.common.ErrorCode;
import faiss.manager.common.exception.FaissManagerException;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 本地磁盘索引文件存储实现
 */
@Slf4j
public class LocalIndexRepository implements IndexRepository {

    private static final String INDEX_FILE_SUFFIX = ".faiss";

    private final Path baseDir;

    public LocalIndexRepository(Path baseDir) {
        this.baseDir = baseDir;
        ensureDirectory();
    }

    @Override
    public void save(String indexId, byte[] data) {
        try {
            Path filePath = resolveIndexPath(indexId);
            Files.write(filePath, data);
            log.info("Saved index to local: {}, size={} bytes", filePath, data.length);
        } catch (IOException e) {
            throw new FaissManagerException(ErrorCode.STORAGE_WRITE_FAILED,
                    "索引文件保存失败: " + e.getMessage(), e);
        }
    }

    @Override
    public byte[] load(String indexId) {
        Path filePath = resolveIndexPath(indexId);
        if (!Files.exists(filePath)) {
            throw new FaissManagerException(ErrorCode.INDEX_NOT_FOUND,
                    "索引文件不存在: " + filePath);
        }
        try {
            byte[] data = Files.readAllBytes(filePath);
            log.info("Loaded index from local: {}, size={} bytes", filePath, data.length);
            return data;
        } catch (IOException e) {
            throw new FaissManagerException(ErrorCode.STORAGE_READ_FAILED,
                    "索引文件读取失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(String indexId) {
        Path filePath = resolveIndexPath(indexId);
        try {
            boolean deleted = Files.deleteIfExists(filePath);
            if (deleted) {
                log.info("Deleted index file: {}", filePath);
            }
        } catch (IOException e) {
            throw new FaissManagerException(ErrorCode.STORAGE_DELETE_FAILED,
                    "索引文件删除失败: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean exists(String indexId) {
        return Files.exists(resolveIndexPath(indexId));
    }

    @Override
    public List<String> listAll() {
        List<String> indexIds = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(baseDir, "*" + INDEX_FILE_SUFFIX)) {
            for (Path path : stream) {
                String fileName = path.getFileName().toString();
                indexIds.add(fileName.substring(0, fileName.length() - INDEX_FILE_SUFFIX.length()));
            }
        } catch (IOException e) {
            log.warn("Failed to list index files in {}", baseDir, e);
        }
        return indexIds;
    }

    private Path resolveIndexPath(String indexId) {
        return baseDir.resolve(indexId + INDEX_FILE_SUFFIX);
    }

    private void ensureDirectory() {
        try {
            Files.createDirectories(baseDir);
        } catch (IOException e) {
            throw new FaissManagerException(ErrorCode.STORAGE_WRITE_FAILED,
                    "无法创建索引存储目录: " + baseDir, e);
        }
    }
}
