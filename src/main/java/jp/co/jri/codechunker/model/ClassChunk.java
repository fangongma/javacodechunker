package jp.co.jri.codechunker.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
        "language", "filePath", "chunkId", "kind", "name", "parent", "signature",
        "location", "imports", "modifiers", "symbols", "code", "notes"
})
public class ClassChunk {
    @JsonIgnore
    private String fullyQualifiedName;

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
