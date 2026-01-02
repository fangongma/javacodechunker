package jp.co.jri.codechunker.model.chunk.data;

import com.fasterxml.jackson.annotation.JsonInclude;
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
        "classes", "methods", "fields", "variables"
})
public class Symbols {
    private List<String> classes;
    private List<String> methods;
    private List<String> fields;
    private List<String> variables;
}
