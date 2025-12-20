package jp.co.jri.codechunker.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "projectPath", "analysisLevel", "timestamp", "totalFiles",
        "totalClasses", "totalMethods", "processedFiles", "errorFiles", "chunks"
})
public class ProjectAnalysisResult<T> {

    @JsonProperty("projectPath")
    private String projectPath;

    @JsonProperty("analysisLevel")
    private String analysisLevel;

    @JsonProperty("timestamp")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;

    @JsonProperty("totalFiles")
    private Integer totalFiles;

    @JsonProperty("totalClasses")
    private Integer totalClasses;

    @JsonProperty("totalMethods")
    private Integer totalMethods;

    @JsonProperty("processedFiles")
    private Integer processedFiles;

    @JsonProperty("errorFiles")
    private Integer errorFiles;

    @JsonProperty("chunks")
    private List<T> chunks;

    public ProjectAnalysisResult() {
        this.timestamp = LocalDateTime.now();
    }
}