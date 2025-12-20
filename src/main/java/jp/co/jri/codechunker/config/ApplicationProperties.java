package jp.co.jri.codechunker.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "code-chunker")
public class ApplicationProperties {

    private ChunkConfig chunk = new ChunkConfig();
    private FilterConfig filter = new FilterConfig();
    private OutputConfig output = new OutputConfig();

    public static class ChunkConfig {
        private String defaultLevel = "CLASS";
        private int maxSnippetLength = 200;
        private boolean includeCodeSnippets = true;

        public String getDefaultLevel() { return defaultLevel; }
        public void setDefaultLevel(String defaultLevel) { this.defaultLevel = defaultLevel; }

        public int getMaxSnippetLength() { return maxSnippetLength; }
        public void setMaxSnippetLength(int maxSnippetLength) { this.maxSnippetLength = maxSnippetLength; }

        public boolean isIncludeCodeSnippets() { return includeCodeSnippets; }
        public void setIncludeCodeSnippets(boolean includeCodeSnippets) { this.includeCodeSnippets = includeCodeSnippets; }
    }

    public static class FilterConfig {
        private List<String> excludePatterns = new ArrayList<>();
        private List<String> excludeDirectories = List.of(
                "target", "bin", "build", ".git", "node_modules", ".idea", ".vscode"
        );

        public List<String> getExcludePatterns() { return excludePatterns; }
        public void setExcludePatterns(List<String> excludePatterns) { this.excludePatterns = excludePatterns; }

        public List<String> getExcludeDirectories() { return excludeDirectories; }
        public void setExcludeDirectories(List<String> excludeDirectories) { this.excludeDirectories = excludeDirectories; }
    }

    public static class OutputConfig {
        private String defaultFormat = "json";
        private boolean prettyPrint = true;
        private String dateFormat = "yyyy-MM-dd'T'HH:mm:ss";

        public String getDefaultFormat() { return defaultFormat; }
        public void setDefaultFormat(String defaultFormat) { this.defaultFormat = defaultFormat; }

        public boolean isPrettyPrint() { return prettyPrint; }
        public void setPrettyPrint(boolean prettyPrint) { this.prettyPrint = prettyPrint; }

        public String getDateFormat() { return dateFormat; }
        public void setDateFormat(String dateFormat) { this.dateFormat = dateFormat; }
    }

    public ChunkConfig getChunk() { return chunk; }
    public void setChunk(ChunkConfig chunk) { this.chunk = chunk; }

    public FilterConfig getFilter() { return filter; }
    public void setFilter(FilterConfig filter) { this.filter = filter; }

    public OutputConfig getOutput() { return output; }
    public void setOutput(OutputConfig output) { this.output = output; }
}