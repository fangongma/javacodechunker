package jp.co.jri.codechunker.service;

import lombok.Data;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.util.List;

@Data
public class AnalysisConfig {
    private String projectPath;
    private String outputPath;
    private ChunkLevel level;
    private List<String> includePatterns;
    private List<String> excludePatterns;
    private boolean includeCodeSnippets;
    private int maxSnippetLength;

    public AnalysisConfig(String projectPath, String outputPath, ChunkLevel level) {
        this.projectPath = projectPath;
        this.outputPath = outputPath;
        this.level = level;
        this.includeCodeSnippets = true;
        this.maxSnippetLength = 200;
    }
}