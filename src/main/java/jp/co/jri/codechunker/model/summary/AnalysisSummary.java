package jp.co.jri.codechunker.model.summary;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class AnalysisSummary {
    private String projectPath;
    private String analysisType; // CLASS_ONLY or METHODS_ONLY
    private String outputDirectory;
    private LocalDateTime timestamp;
    private Integer totalFiles;
    private Integer totalClasses;
    private Integer totalMethods;
    private Integer processedFiles;
    private Integer errorFiles;
    private List<OutputFileInfo> outputFiles;

    public AnalysisSummary() {
        this.outputFiles = new ArrayList<>();
        this.timestamp = LocalDateTime.now();
    }

    public void addClassFile(String fileName, String fullyQualifiedName,
                             String type, Integer count, String fileType) {
        OutputFileInfo info = new OutputFileInfo();
        info.setFileName(fileName);
        info.setFullyQualifiedName(fullyQualifiedName);
        info.setType(type);
        info.setCount(count);
        info.setFileType(fileType);
        this.outputFiles.add(info);
    }
}
