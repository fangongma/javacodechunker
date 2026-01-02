package jp.co.jri.codechunker.runner;

import jp.co.jri.codechunker.config.ApplicationProperties;
import jp.co.jri.codechunker.model.chunk.ChunkLevel;
import com.fasterxml.jackson.databind.ObjectMapper;
import jp.co.jri.codechunker.model.summary.AnalysisSummary;
import jp.co.jri.codechunker.model.summary.OutputFileInfo;
import jp.co.jri.codechunker.service.JavaCodeChunkerService;
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
    private final JavaCodeChunkerService javaCodeChunkerService;
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
                formatter.printHelp("javacodechunker", options);
                return;
            }

            // Get required arguments
            String projectPath = cmd.getOptionValue("project");

            if (projectPath == null) {
                System.err.println("Error: Project path is required");
                formatter.printHelp("javacodechunker", options);
                System.exit(1);
            }

            // Validate project path
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
            logger.info("  Level: {}", level.getValue());
            logger.info("  Include patterns: {}", includePatterns.isEmpty() ? "none" : includePatterns);
            logger.info("  Exclude patterns: {}", excludePatterns.isEmpty() ? "none" : excludePatterns);

            // Check output mode
            String outputPath = cmd.getOptionValue("output");
            boolean perClassMode = cmd.hasOption("per-class");

            if (perClassMode) {
                // Per-class output mode - one JSON file per class
                if (outputPath == null) {
                    // Use default output directory based on analysis type
                    String defaultDir = level == ChunkLevel.METHOD_LEVEL ?
                            "output/methods-per-class" : "output/classes-per-class";
                    outputPath = defaultDir;
                }

                logger.info("  Output mode: One JSON file per class");
                logger.info("  Output directory: {}", outputPath);

                AnalysisSummary summary;

                if (level == ChunkLevel.CLASS_LEVEL) {
                    // Generate class-only files
                    summary = javaCodeChunkerService.generateClassFiles(
                            projectPath, includePatterns, excludePatterns, outputPath);
                } else {
                    // Generate method-only files
                    summary = javaCodeChunkerService.generateMethodFiles(
                            projectPath, includePatterns, excludePatterns, outputPath);
                }

                printPerClassSummary(summary, level);

            } else {
                logger.info("'per-class' option is missing!");
            }

            logger.info("Analysis completed successfully!");

        } catch (ParseException e) {
            System.err.println("Error parsing command line: " + e.getMessage());
            formatter.printHelp("javacodechunker", options);
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
                .argName("PATH")
                .desc("Output file path (for single file mode) or directory (for per-class mode)")
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

        options.addOption(Option.builder("pc")
                .longOpt("per-class")
                .desc("Generate one JSON file per class")
                .build());

        options.addOption(Option.builder("h")
                .longOpt("help")
                .desc("Show this help message")
                .build());

        return options;
    }

    private void printPerClassSummary(AnalysisSummary summary, ChunkLevel level) {
        String separator = "=".repeat(60);

        System.out.println(separator);
        System.out.println("ANALYSIS COMPLETED SUCCESSFULLY");
        System.out.println(separator);

        System.out.println("\nPer-Class Analysis Summary:");
        System.out.printf("  Analysis Type: %s%n", summary.getAnalysisType());
        System.out.printf("  Project: %s%n", summary.getProjectPath());
        System.out.printf("  Output Directory: %s%n", summary.getOutputDirectory());
        System.out.printf("  Timestamp: %s%n", summary.getTimestamp());
        System.out.printf("  Total Files Scanned: %d%n", summary.getTotalFiles());
        System.out.printf("  Processed Files: %d%n", summary.getProcessedFiles());
        System.out.printf("  Files with Errors: %d%n", summary.getErrorFiles());
        System.out.printf("  Total Output Files: %d%n", summary.getTotalClasses());

        if (level == ChunkLevel.METHOD_LEVEL) {
            System.out.printf("  Total Methods: %d%n", summary.getTotalMethods());
        }

        System.out.println("\nGenerated JSON Files:");
        int count = 0;
        for (OutputFileInfo fileInfo : summary.getOutputFiles()) {
            if (count < 10) { // Show first 10 files
                String countInfo = level == ChunkLevel.METHOD_LEVEL ?
                        String.format(" (%d methods)", fileInfo.getCount()) :
                        String.format(" (class)");
                System.out.printf("  - %s -> %s [%s]%s%n",
                        fileInfo.getFileName(),
                        fileInfo.getFullyQualifiedName(),
                        fileInfo.getType(),
                        countInfo);
            }
            count++;
        }

        if (summary.getOutputFiles().size() > 10) {
            System.out.printf("  ... and %d more files%n", summary.getOutputFiles().size() - 10);
        }

        System.out.println("\nFiles have been saved to: " + summary.getOutputDirectory());
        System.out.println("Project summary saved to: " + summary.getOutputDirectory() + "/project-summary.json");
        System.out.println(separator);
    }
}