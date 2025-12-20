package jp.co.jri.codechunker.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "chunkId", "chunkType", "methodName", "className", "packageName",
        "filePath", "fullyQualifiedName", "startLine", "endLine", "totalLines",
        "returnType", "modifiers", "parameters", "parameterCount", "annotations",
        "throwsDeclarations", "lineCount", "cyclomaticComplexity", "methodCalls", "codeSnippet"
})
public class MethodChunk {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Parameter {
        @JsonProperty("name")
        private String name;

        @JsonProperty("type")
        private String type;
    }

    @JsonProperty("chunkId")
    private String chunkId;

    @JsonProperty("chunkType")
    private String chunkType = "METHOD";

    @JsonProperty("methodName")
    private String methodName;

    @JsonProperty("className")
    private String className;

    @JsonProperty("package")
    private String packageName;

    @JsonProperty("filePath")
    private String filePath;

    @JsonProperty("fullyQualifiedName")
    private String fullyQualifiedName;

    @JsonProperty("startLine")
    private Integer startLine;

    @JsonProperty("endLine")
    private Integer endLine;

    @JsonProperty("totalLines")
    private Integer totalLines;

    @JsonProperty("returnType")
    private String returnType;

    @JsonProperty("modifiers")
    private List<String> modifiers;

    @JsonProperty("parameters")
    private List<Parameter> parameters;

    @JsonProperty("parameterCount")
    private Integer parameterCount;

    @JsonProperty("annotations")
    private List<String> annotations;

    @JsonProperty("throwsDeclarations")
    private List<String> throwsDeclarations;

    @JsonProperty("lineCount")
    private Integer lineCount;

    @JsonProperty("cyclomaticComplexity")
    private Integer cyclomaticComplexity;

    @JsonProperty("methodCalls")
    private List<String> methodCalls;

    @JsonProperty("codeSnippet")
    private String codeSnippet;
}
