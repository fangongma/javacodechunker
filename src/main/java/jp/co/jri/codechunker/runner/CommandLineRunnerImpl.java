package jp.co.jri.codechunker.runner;

import jp.co.jri.codechunker.config.ApplicationProperties;
import jp.co.jri.codechunker.model.ClassChunk;
import jp.co.jri.codechunker.model.MethodChunk;
import jp.co.jri.codechunker.model.ProjectAnalysisResult;
import jp.co.jri.codechunker.service.ChunkLevel;
import jp.co.jri.codechunker.service.JavaProjectChunkerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CommandLineRunnerImpl implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(CommandLineRunnerImpl.class);
    private final JavaProjectChunkerService chunkerService;
    private final ApplicationProperties properties;
    private final ObjectMapper objectMapper;

    @Override
    public void run(String... args) throws Exception {
        Options options = createOptions();
        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();

        try {
            CommandLine cmd = parser.parse(options, args);

            if (cmd.hasOption("help")) {
                formatter.printHelp("java-code-chunker", options);
                return;
            }

            // Get required arguments
            String projectPath = cmd.getOptionValue("project");
            String outputPath = cmd.getOptionValue("output");

            if (projectPath == null || outputPath == null) {
                System.err.println("Error: Project path and output path are required");
                formatter.printHelp("java-code-chunker", options);
                System.exit(1);
            }

            // Validate paths
            if (!Paths.get(projectPath).toFile().exists()) {
                logger.error("Project path does not exist: {}", projectPath);
                System.exit(1);
            }

            // Configure analysis
            ChunkLevel level = cmd.hasOption("method") ?
                    ChunkLevel.METHOD_LEVEL :
                    ChunkLevel.fromString(properties.getChunk().getDefaultLevel());

            List<String> includePatterns = new ArrayList<>();
            if (cmd.hasOption("include")) {
                includePatterns = List.of(cmd.getOptionValues("include"));
            }

            List<String> excludePatterns = new ArrayList<>(properties.getFilter().getExcludePatterns());
            if (cmd.hasOption("exclude")) {
                excludePatterns.addAll(List.of(cmd.getOptionValues("exclude")));
            }

            logger.info("Starting analysis with configuration:");
            logger.info("  Project: {}", projectPath);
            logger.info("  Output: {}", outputPath);
            logger.info("  Level: {}", level.getValue());
            logger.info("  Include patterns: {}", includePatterns.isEmpty() ? "none" : includePatterns);
            logger.info("  Exclude patterns: {}", excludePatterns.isEmpty() ? "none" : excludePatterns);

            // Run analysis
            if (level == ChunkLevel.CLASS_LEVEL) {
                ProjectAnalysisResult<ClassChunk> result = chunkerService.analyzeAtClassLevel(
                        projectPath, includePatterns, excludePatterns);
                chunkerService.saveReportToJson(result, outputPath);
                printSummary(result);
            } else {
                ProjectAnalysisResult<MethodChunk> result = chunkerService.analyzeAtMethodLevel(
                        projectPath, includePatterns, excludePatterns);
                chunkerService.saveReportToJson(result, outputPath);
                printSummary(result);
            }

            logger.info("Analysis completed successfully!");

        } catch (ParseException e) {
            System.err.println("Error parsing command line: " + e.getMessage());
            formatter.printHelp("java-code-chunker", options);
            System.exit(1);
        } catch (Exception e) {
            logger.error("Error during analysis: {}", e.getMessage(), e);
            System.exit(1);
        }
    }

    private Options createOptions() {
        Options options = new Options();

        options.addOption(Option.builder("p")
                .longOpt("project")
                .hasArg()
                .argName("PATH")
                .desc("Path to Java project directory (required)")
                .required()
                .build());

        options.addOption(Option.builder("o")
                .longOpt("output")
                .hasArg()
                .argName("FILE")
                .desc("Output JSON file path (required)")
                .required()
                .build());

        options.addOption(Option.builder("m")
                .longOpt("method")
                .desc("Analyze at method level (default: class level)")
                .build());

        options.addOption(Option.builder("i")
                .longOpt("include")
                .hasArg()
                .argName("PATTERN")
                .desc("Include files matching regex pattern (can be used multiple times)")
                .build());

        options.addOption(Option.builder("e")
                .longOpt("exclude")
                .hasArg()
                .argName("PATTERN")
                .desc("Exclude files matching regex pattern (can be used multiple times)")
                .build());

        options.addOption(Option.builder("h")
                .longOpt("help")
                .desc("Show this help message")
                .build());

        return options;
    }

    private void printSummary(ProjectAnalysisResult<?> result) {
        String separator = "=".repeat(60);

        System.out.println(separator);
        System.out.println("ANALYSIS COMPLETED SUCCESSFULLY");
        System.out.println(separator);

        try {
            String summaryJson = objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(result);

            System.out.println("\nSummary (JSON format):");
            System.out.println(summaryJson);

        } catch (Exception e) {
            // Fallback to simple text summary
            System.out.printf("\nSummary:%n");
            System.out.printf("  Project: %s%n", result.getProjectPath());
            System.out.printf("  Analysis Level: %s%n", result.getAnalysisLevel());
            System.out.printf("  Timestamp: %s%n", result.getTimestamp());
            System.out.printf("  Total Files: %d%n", result.getTotalFiles());
            System.out.printf("  Processed Files: %d%n", result.getProcessedFiles());
            System.out.printf("  Files with Errors: %d%n", result.getErrorFiles());

            if (result.getAnalysisLevel().equals("CLASS_LEVEL")) {
                ProjectAnalysisResult<ClassChunk> classResult = (ProjectAnalysisResult<ClassChunk>) result;
                System.out.printf("  Total Classes: %d%n", classResult.getTotalClasses());
            } else {
                ProjectAnalysisResult<MethodChunk> methodResult = (ProjectAnalysisResult<MethodChunk>) result;
                System.out.printf("  Total Methods: %d%n", methodResult.getTotalMethods());
            }

            System.out.printf("  Total Chunks Generated: %d%n",
                    result.getChunks() != null ? result.getChunks().size() : 0);
        }

        System.out.println("\nReport has been saved successfully!");
        System.out.println(separator);
    }
}