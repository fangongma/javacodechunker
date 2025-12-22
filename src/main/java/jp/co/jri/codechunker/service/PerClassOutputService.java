package jp.co.jri.codechunker.service;

import jp.co.jri.codechunker.config.ApplicationProperties;
import jp.co.jri.codechunker.model.*;
import jp.co.jri.codechunker.util.FileFinder;
import jp.co.jri.codechunker.util.MetricsCalculator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class PerClassOutputService {

    private static final Logger logger = LoggerFactory.getLogger(PerClassOutputService.class);
    private final JavaParser javaParser = new JavaParser();
    private final FileFinder fileFinder;
    private final MetricsCalculator metricsCalculator;
    private final ObjectMapper objectMapper;
    private final ApplicationProperties properties;

    /**
     * Analyzes project and generates one JSON file per class (class-level analysis)
     * Each file contains only class information
     */
    public PerClassAnalysisSummary generateClassFiles(String projectPath,
                                                      List<String> includePatterns,
                                                      List<String> excludePatterns,
                                                      String outputDir) throws IOException {

        logger.info("Generating class files for project: {}", projectPath);
        logger.info("Output directory: {}", outputDir);
        logger.info("Mode: One JSON file per class (class info only)");

        // Create output directory if it doesn't exist
        File outputDirectory = new File(outputDir);
        if (!outputDirectory.exists()) {
            boolean created = outputDirectory.mkdirs();
            if (created) {
                logger.info("Created output directory: {}", outputDir);
            }
        }

        List<Path> javaFiles = fileFinder.findJavaFiles(projectPath, includePatterns, excludePatterns);

        PerClassAnalysisSummary summary = new PerClassAnalysisSummary();
        summary.setProjectPath(projectPath);
        summary.setAnalysisType("CLASS_ONLY");
        summary.setOutputDirectory(outputDir);
        summary.setTimestamp(LocalDateTime.now());
        summary.setTotalFiles(javaFiles.size());

        int processedFiles = 0;
        int errorFiles = 0;
        int totalClasses = 0;

        for (Path javaFile : javaFiles) {
            logger.debug("Processing file: {}", javaFile);

            try {
                ParseResult<CompilationUnit> parseResult = javaParser.parse(javaFile);

                if (parseResult.isSuccessful() && parseResult.getResult().isPresent()) {
                    CompilationUnit cu = parseResult.getResult().get();
                    List<ClassChunk> classChunks = extractClassChunksFromFile(cu, javaFile);

                    // Save each class to individual JSON file
                    for (ClassChunk classChunk : classChunks) {
                        saveClassChunkToJson(classChunk, outputDir);
                        totalClasses++;

//                        summary.addClassFile(
//                                generateFileName(classChunk.getFullyQualifiedName(), "class"),
//                                classChunk.getFullyQualifiedName(),
//                                classChunk.getType(),
//                                classChunk.getMethodCount(),
//                                "CLASS"
//                        );

                        summary.addClassFile(
                                "fullyqualifiedname",
                                "fullyqualifiedname",
                                "class",
                                999,
                                "CLASS"
                        );
                    }

                    processedFiles++;

                    if (processedFiles % 10 == 0) {
                        logger.info("Processed {} files, generated {} class files...",
                                processedFiles, totalClasses);
                    }
                } else {
                    logger.warn("Failed to parse file: {}", javaFile);
                    errorFiles++;
                }
            } catch (Exception e) {
                logger.error("Error parsing file: {} - {}", javaFile, e.getMessage(), e);
                errorFiles++;
            }
        }

        summary.setProcessedFiles(processedFiles);
        summary.setErrorFiles(errorFiles);
        summary.setTotalClasses(totalClasses);

        // Save project summary
        saveProjectSummary(summary, outputDir);

        logger.info("Class file generation completed:");
        logger.info("  Processed files: {}", processedFiles);
        logger.info("  Error files: {}", errorFiles);
        logger.info("  Total classes: {}", totalClasses);
        logger.info("  Output directory: {}", outputDir);

        return summary;
    }

    /**
     * Analyzes project and generates one JSON file per class (method-level analysis)
     * Each file contains only method information for that class
     */
    public PerClassAnalysisSummary generateMethodFiles(String projectPath,
                                                       List<String> includePatterns,
                                                       List<String> excludePatterns,
                                                       String outputDir) throws IOException {

        logger.info("Generating method files for project: {}", projectPath);
        logger.info("Output directory: {}", outputDir);
        logger.info("Mode: One JSON file per class (methods only)");

        // Create output directory if it doesn't exist
        File outputDirectory = new File(outputDir);
        if (!outputDirectory.exists()) {
            boolean created = outputDirectory.mkdirs();
            if (created) {
                logger.info("Created output directory: {}", outputDir);
            }
        }

        List<Path> javaFiles = fileFinder.findJavaFiles(projectPath, includePatterns, excludePatterns);

        PerClassAnalysisSummary summary = new PerClassAnalysisSummary();
        summary.setProjectPath(projectPath);
        summary.setAnalysisType("METHODS_ONLY");
        summary.setOutputDirectory(outputDir);
        summary.setTimestamp(LocalDateTime.now());
        summary.setTotalFiles(javaFiles.size());

        int processedFiles = 0;
        int errorFiles = 0;
        int totalClasses = 0;
        int totalMethods = 0;

        for (Path javaFile : javaFiles) {
            logger.debug("Processing file: {}", javaFile);

            try {
                ParseResult<CompilationUnit> parseResult = javaParser.parse(javaFile);

                if (parseResult.isSuccessful() && parseResult.getResult().isPresent()) {
                    CompilationUnit cu = parseResult.getResult().get();
                    List<ClassMethods> classMethodsList = extractMethodsFromFile(cu, javaFile);

                    // Save each class's methods to individual JSON file
                    for (ClassMethods classMethods : classMethodsList) {
                        if (!classMethods.getMethods().isEmpty()) {
                            saveMethodsToJson(classMethods, outputDir);
                            totalClasses++;
                            totalMethods += classMethods.getMethods().size();

                            summary.addClassFile(
                                    generateFileName(classMethods.getFullyQualifiedName(), "methods"),
                                    classMethods.getFullyQualifiedName(),
                                    classMethods.getType(),
                                    classMethods.getMethods().size(),
                                    "METHODS"
                            );
                        }
                    }

                    processedFiles++;

                    if (processedFiles % 10 == 0) {
                        logger.info("Processed {} files, generated {} method files...",
                                processedFiles, totalClasses);
                    }
                } else {
                    logger.warn("Failed to parse file: {}", javaFile);
                    errorFiles++;
                }
            } catch (Exception e) {
                logger.error("Error parsing file: {} - {}", javaFile, e.getMessage(), e);
                errorFiles++;
            }
        }

        summary.setProcessedFiles(processedFiles);
        summary.setErrorFiles(errorFiles);
        summary.setTotalClasses(totalClasses);
        summary.setTotalMethods(totalMethods);

        // Save project summary
        saveProjectSummary(summary, outputDir);

        logger.info("Method file generation completed:");
        logger.info("  Processed files: {}", processedFiles);
        logger.info("  Error files: {}", errorFiles);
        logger.info("  Total classes with methods: {}", totalClasses);
        logger.info("  Total methods: {}", totalMethods);
        logger.info("  Output directory: {}", outputDir);

        return summary;
    }

    /**
     * Extracts class chunks from a file
     */
    private List<ClassChunk> extractClassChunksFromFile(CompilationUnit cu, Path filePath) {
        List<ClassChunk> classChunks = new ArrayList<>();
        String packageName = cu.getPackageDeclaration()
                .map(p -> p.getNameAsString())
                .orElse("");

        cu.accept(new VoidVisitorAdapter<Void>() {
            @Override
            public void visit(ClassOrInterfaceDeclaration n, Void arg) {
                ClassChunk chunk = createClassChunk(n, filePath, packageName,
                        n.isInterface() ? "INTERFACE" : "CLASS");
                classChunks.add(chunk);
                super.visit(n, arg);
            }

            @Override
            public void visit(EnumDeclaration n, Void arg) {
                ClassChunk chunk = createClassChunk(n, filePath, packageName, "ENUM");
                classChunks.add(chunk);
                super.visit(n, arg);
            }

            @Override
            public void visit(AnnotationDeclaration n, Void arg) {
                ClassChunk chunk = createClassChunk(n, filePath, packageName, "ANNOTATION");
                classChunks.add(chunk);
                super.visit(n, arg);
            }
        }, null);

        return classChunks;
    }

    /**
     * Extracts methods from a file, grouped by class
     */
    private List<ClassMethods> extractMethodsFromFile(CompilationUnit cu, Path filePath) {
        List<ClassMethods> classMethodsList = new ArrayList<>();
        String packageName = cu.getPackageDeclaration()
                .map(p -> p.getNameAsString())
                .orElse("");

        Map<String, List<MethodChunk>> methodsByClass = new HashMap<>();

        // Find all methods and group them by containing class
        List<MethodDeclaration> allMethods = cu.findAll(MethodDeclaration.class);
        for (MethodDeclaration method : allMethods) {
            // Find containing class
            Optional<ClassOrInterfaceDeclaration> containingClass =
                    method.findAncestor(ClassOrInterfaceDeclaration.class);

            String className = containingClass.map(ClassOrInterfaceDeclaration::getNameAsString).orElse("");
            String fullyQualifiedName = packageName.isEmpty() ?
                    className : packageName + "." + className;

            String type = "CLASS";
            if (containingClass.isPresent()) {
                type = containingClass.get().isInterface() ? "INTERFACE" : "CLASS";
            }

            // Get or create class methods entry
            ClassMethods classMethods = findOrCreateClassMethods(
                    classMethodsList, fullyQualifiedName, className, packageName, type, filePath);

            // Create method chunk
            MethodChunk methodChunk = createMethodChunk(method, filePath, packageName);
            classMethods.getMethods().add(methodChunk);
        }

        // Also handle methods in enums
        List<EnumDeclaration> enums = cu.findAll(EnumDeclaration.class);
        for (EnumDeclaration enumDecl : enums) {
            String className = enumDecl.getNameAsString();
            String fullyQualifiedName = packageName.isEmpty() ?
                    className : packageName + "." + className;

            ClassMethods classMethods = findOrCreateClassMethods(
                    classMethodsList, fullyQualifiedName, className, packageName, "ENUM", filePath);

            for (MethodDeclaration method : enumDecl.getMethods()) {
                MethodChunk methodChunk = createMethodChunk(method, filePath, packageName);
                classMethods.getMethods().add(methodChunk);
            }
        }

        return classMethodsList;
    }

    /**
     * Finds or creates a ClassMethods entry
     */
    private ClassMethods findOrCreateClassMethods(List<ClassMethods> classMethodsList,
                                                  String fullyQualifiedName,
                                                  String className,
                                                  String packageName,
                                                  String type,
                                                  Path filePath) {
        for (ClassMethods cm : classMethodsList) {
            if (cm.getFullyQualifiedName().equals(fullyQualifiedName)) {
                return cm;
            }
        }

        ClassMethods newClassMethods = ClassMethods.builder()
                .fullyQualifiedName(fullyQualifiedName)
                .className(className)
                .packageName(packageName)
                .type(type)
                .sourceFile(filePath.toString())
                .timestamp(LocalDateTime.now())
                .methods(new ArrayList<>())
                .build();

        classMethodsList.add(newClassMethods);
        return newClassMethods;
    }

    /**
     * Creates a ClassChunk from TypeDeclaration
     */
    private ClassChunk createClassChunk(TypeDeclaration<?> typeDecl, Path filePath,
                                        String packageName, String type) {
        String className = typeDecl.getNameAsString();

        ClassChunk.ClassChunkBuilder builder = ClassChunk.builder();
                //.chunkId(generateChunkId(filePath, className, type))
                //.className(className)
                //.type(type)
                //.packageName(packageName)
                //.filePath(filePath.toAbsolutePath().toString())
                //.fullyQualifiedName(packageName.isEmpty() ? className : packageName + "." + className);

        // Line information
        typeDecl.getRange().ifPresent(range -> {
//            builder.startLine(range.begin.line)
//                    .endLine(range.end.line)
//                    .totalLines(range.end.line - range.begin.line + 1);
            builder.location(new Location(range.begin.line, range.end.line));
        });

        // Modifiers
        List<String> modifiers = new ArrayList<>();
        for (com.github.javaparser.ast.Modifier mod : typeDecl.getModifiers()) {
            modifiers.add(mod.toString());
        }
        builder.modifiers(modifiers);
        //builder.modifiers2(modifiers);

        // Counts
//        builder.methodCount(typeDecl.getMethods().size())
//                .fieldCount(typeDecl.getFields().size());

        // Dependencies
        List<String> dependencies = new ArrayList<>();
        typeDecl.findCompilationUnit().ifPresent(cu -> {
            for (com.github.javaparser.ast.ImportDeclaration imp : cu.getImports()) {
                dependencies.add(imp.getNameAsString());
            }
        });
//        builder.dependencies(dependencies);
        builder.imports(dependencies);

        // Inheritance
        if (typeDecl instanceof ClassOrInterfaceDeclaration) {
            ClassOrInterfaceDeclaration classDecl = (ClassOrInterfaceDeclaration) typeDecl;
            List<String> extendedClasses = new ArrayList<>();
            for (com.github.javaparser.ast.type.ClassOrInterfaceType ext : classDecl.getExtendedTypes()) {
                extendedClasses.add(ext.getNameAsString());
            }

            List<String> implementedInterfaces = new ArrayList<>();
            for (com.github.javaparser.ast.type.ClassOrInterfaceType imp : classDecl.getImplementedTypes()) {
                implementedInterfaces.add(imp.getNameAsString());
            }

            builder.parent(new ParentRef(packageName, Stream.concat(extendedClasses.stream(), implementedInterfaces.stream()).collect(Collectors.toList())));
        }

        // Code snippet
        String code = typeDecl.toString();
        int maxSnippetLength = properties.getChunk().getMaxSnippetLength();
        logger.debug("maxSnippetLength = ", maxSnippetLength);
        logger.debug("code length = ", code);
//        builder.codeSnippet(code.length() > maxSnippetLength ?
//                code.substring(0, maxSnippetLength) + "..." : code);
        builder.code(code);

        // Complexity
//        builder.complexityScore(metricsCalculator.calculateComplexity(typeDecl));

        Symbols.SymbolsBuilder symbolsBuilder = Symbols.builder();
        symbolsBuilder.classes(null);
        symbolsBuilder.methods(null);
        symbolsBuilder.fields(null);
        symbolsBuilder.variables(null);

        Notes.NotesBuilder notesBuilder = Notes.builder();
        notesBuilder.missingData(null);
        notesBuilder.extractionWarnings(null);

        builder.language("java")
                .filePath(filePath.toAbsolutePath().toString())
                .chunkId(packageName)
                .kind(Kind.CLASS)
                .name(className)
                //.parent(packageName.isEmpty() ? className : packageName + "." + className)
                .signature("siganture")
                //.location()
                //.imports()
                //.modifiers2()
                .symbols(symbolsBuilder.build())
                //.code2()
                .notes(notesBuilder.build());

        return builder.build();
    }

    /**
     * Creates a MethodChunk from MethodDeclaration
     */
    private MethodChunk createMethodChunk(MethodDeclaration method, Path filePath, String packageName) {
        String methodName = method.getNameAsString();

        // Find containing class
        Optional<ClassOrInterfaceDeclaration> containingClass =
                method.findAncestor(ClassOrInterfaceDeclaration.class);
        String className = containingClass.map(ClassOrInterfaceDeclaration::getNameAsString).orElse("");

        MethodChunk.MethodChunkBuilder builder = MethodChunk.builder()
                .chunkId(generateChunkId(filePath, methodName, "METHOD"))
                .methodName(methodName)
                .className(className)
                .packageName(packageName)
                .filePath(filePath.toAbsolutePath().toString())
                .fullyQualifiedName(packageName.isEmpty() ?
                        className + "." + methodName :
                        packageName + "." + className + "." + methodName);

        // Line information
        method.getRange().ifPresent(range -> {
            builder.startLine(range.begin.line)
                    .endLine(range.end.line)
                    .totalLines(range.end.line - range.begin.line + 1);
        });

        // Return type
        builder.returnType(method.getType().toString());

        // Modifiers
        List<String> modifiers = new ArrayList<>();
        for (com.github.javaparser.ast.Modifier mod : method.getModifiers()) {
            modifiers.add(mod.toString());
        }
        builder.modifiers(modifiers);

        // Parameters
        List<MethodChunk.Parameter> parameters = new ArrayList<>();
        for (com.github.javaparser.ast.body.Parameter param : method.getParameters()) {
            MethodChunk.Parameter paramObj = MethodChunk.Parameter.builder()
                    .name(param.getNameAsString())
                    .type(param.getType().toString())
                    .build();
            parameters.add(paramObj);
        }
        builder.parameters(parameters)
                .parameterCount(parameters.size());

        // Annotations
        List<String> annotations = new ArrayList<>();
        for (com.github.javaparser.ast.expr.AnnotationExpr ann : method.getAnnotations()) {
            annotations.add(ann.getNameAsString());
        }
        builder.annotations(annotations);

        // Throws declarations
        List<String> throwsList = new ArrayList<>();
        for (com.github.javaparser.ast.type.ReferenceType ex : method.getThrownExceptions()) {
            throwsList.add(ex.toString());
        }
        builder.throwsDeclarations(throwsList);

        // Code metrics
        String methodBody = method.getBody().map(Object::toString).orElse("");
        builder.lineCount(methodBody.split("\r\n|\r|\n").length)
                .cyclomaticComplexity(metricsCalculator.calculateCyclomaticComplexity(method));

        // Method calls
        List<String> methodCalls = new ArrayList<>();
        for (com.github.javaparser.ast.expr.MethodCallExpr call :
                method.findAll(com.github.javaparser.ast.expr.MethodCallExpr.class)) {
            methodCalls.add(call.getNameAsString());
        }
        builder.methodCalls(methodCalls);

        // Code snippet
        builder.codeSnippet(methodBody.length() > 150 ?
                methodBody.substring(0, 150) + "..." : methodBody);

        return builder.build();
    }

    /**
     * Saves a single class chunk to JSON file (class-level output)
     */
    private void saveClassChunkToJson(ClassChunk classChunk, String outputDir) throws IOException {
        //String fileName = generateFileName(classChunk.getFullyQualifiedName(), "class");
        String fileName = "test.class";
        File outputFile = new File(outputDir, fileName);

        try (FileWriter writer = new FileWriter(outputFile)) {
            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(writer, classChunk);
        }

        logger.debug("Saved class chunk to: {}", outputFile.getAbsolutePath());
    }

    /**
     * Saves methods of a single class to JSON file (method-level output)
     */
    private void saveMethodsToJson(ClassMethods classMethods, String outputDir) throws IOException {
        String fileName = generateFileName(classMethods.getFullyQualifiedName(), "methods");
        File outputFile = new File(outputDir, fileName);

        try (FileWriter writer = new FileWriter(outputFile)) {
            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(writer, classMethods);
        }

        logger.debug("Saved methods to: {}", outputFile.getAbsolutePath());
    }

    /**
     * Saves project summary to JSON file
     */
    private void saveProjectSummary(PerClassAnalysisSummary summary, String outputDir) throws IOException {
        File summaryFile = new File(outputDir, "project-summary.json");

        try (FileWriter writer = new FileWriter(summaryFile)) {
            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(writer, summary);
        }

        logger.info("Saved project summary to: {}", summaryFile.getAbsolutePath());
    }

    /**
     * Generates a unique chunk ID
     */
    private String generateChunkId(Path filePath, String name, String type) {
        String filename = filePath.getFileName().toString().replace(".java", "");
        return String.format("%s_%s_%s_%d",
                filename, name, type, System.currentTimeMillis());
    }

    /**
     * Generates a safe filename from fully qualified class name
     */
    private String generateFileName(String fullyQualifiedName, String suffix) {
        // Replace dots with underscores, replace $ for inner classes, and add .json extension
        String baseName = fullyQualifiedName.replace('.', '_').replace('$', '_');
        return baseName + "_" + suffix + ".json";
    }

    /**
     * Inner class for methods grouped by class
     */
    @lombok.Data
    @lombok.Builder
    public static class ClassMethods {
        private String fullyQualifiedName;
        private String className;
        private String packageName;
        private String type;
        private String sourceFile;
        private LocalDateTime timestamp;
        private List<MethodChunk> methods;
    }

    /**
     * Inner class for per-class project summary
     */
    @lombok.Data
    public static class PerClassAnalysisSummary {
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

        public PerClassAnalysisSummary() {
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

        @lombok.Data
        public static class OutputFileInfo {
            private String fileName;
            private String fullyQualifiedName;
            private String type;
            private Integer count;
            private String fileType; // "CLASS" or "METHODS"
        }
    }
}