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
        //"chunkId", "chunkType", "className", "type", "packageName",
        //"filePath", "fullyQualifiedName", "startLine", "endLine",
        //"totalLines", "modifiers", "methodCount", "fieldCount",
        //"dependencies", "extendedClasses", "implementedInterfaces",
        //"codeSnippet", "complexityScore",
        "language", "filePath2", "chunkId2",
        "kind", "name", "parent", "signature", "location", "imports",
        "modifiers2", "symbols", "code2", "notes"
})
public class ClassChunk {

//    @JsonProperty("chunkId")
//    private String chunkId;
//
//    @JsonProperty("chunkType")
//    private String chunkType = "CLASS";
//
//    @JsonProperty("className")
//    private String className;
//
//    @JsonProperty("type")
//    private String type; // CLASS, INTERFACE, ENUM, ANNOTATION
//
//    @JsonProperty("package")
//    private String packageName;
//
//    @JsonProperty("filePath")
//    private String filePath;
//
//    @JsonProperty("fullyQualifiedName")
//    private String fullyQualifiedName;
//
//    @JsonProperty("startLine")
//    private Integer startLine;
//
//    @JsonProperty("endLine")
//    private Integer endLine;
//
//    @JsonProperty("totalLines")
//    private Integer totalLines;
//
//    @JsonProperty("modifiers")
//    private List<String> modifiers;
//
//    @JsonProperty("methodCount")
//    private Integer methodCount;
//
//    @JsonProperty("fieldCount")
//    private Integer fieldCount;
//
//    @JsonProperty("dependencies")
//    private List<String> dependencies;
//
//    @JsonProperty("extendedClasses")
//    private List<String> extendedClasses;
//
//    @JsonProperty("implementedInterfaces")
//    private List<String> implementedInterfaces;
//
//    @JsonProperty("codeSnippet")
//    private String codeSnippet;
//
//    @JsonProperty("complexityScore")
//    private Integer complexityScore;

    // list fields needed
    @JsonProperty("language")
    private String language;

    @JsonProperty("filePath")
    private String filePath;

    @JsonProperty("chunkId")
    private String chunkId;

    @JsonProperty("kind")
    private Kind kind;

    @JsonProperty("name")
    private String name;

    @JsonProperty("parent")
    private ParentRef parent;

    @JsonProperty("signature")
    private String signature;

    @JsonProperty("location")
    private Location location;

    @JsonProperty("imports")
    private List<String> imports;

    @JsonProperty("modifiers")
    private List<String> modifiers;

    @JsonProperty("symbols")
    private Symbols symbols;

    @JsonProperty("code")
    private String code;

    @JsonProperty("notes")
    private Notes notes;
}
