package jp.co.jri.codechunker.model.summary;

import lombok.Data;

@Data
public class OutputFileInfo {
    private String fileName;
    private String fullyQualifiedName;
    private String type;
    private Integer count;
    private String fileType; // "CLASS" or "METHODS"
}