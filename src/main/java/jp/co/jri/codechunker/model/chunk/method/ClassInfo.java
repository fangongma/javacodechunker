package jp.co.jri.codechunker.model.chunk.method;

import jp.co.jri.codechunker.model.chunk.ChunkData;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Inner class for methods grouped by class
 */
@Data
@Builder
public class ClassInfo {
    private String fullyQualifiedName;
    private String className;
    private String packageName;
    private String type;
    private String sourceFile;
    private LocalDateTime timestamp;
    private List<ChunkData> methods;
}