package jp.co.jri.codechunker.service;

import com.github.javaparser.ast.Node;
import jp.co.jri.codechunker.config.ApplicationProperties;
import jp.co.jri.codechunker.model.summary.AnalysisSummary;
import jp.co.jri.codechunker.model.chunk.ChunkData;
import jp.co.jri.codechunker.model.chunk.data.Kind;
import jp.co.jri.codechunker.model.chunk.data.ParentRef;
import jp.co.jri.codechunker.model.chunk.data.Location;
import jp.co.jri.codechunker.model.chunk.data.Notes;
import jp.co.jri.codechunker.model.chunk.data.Symbols;
import jp.co.jri.codechunker.model.chunk.method.ClassInfo;
import jp.co.jri.codechunker.util.FileFinder;
import jp.co.jri.codechunker.util.MetricsCalculator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import jp.co.jri.codechunker.util.SignatureExtractor;
import jp.co.jri.codechunker.util.SymbolsExtractor;
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
public class JavaCodeChunkerService {

    private static final Logger logger = LoggerFactory.getLogger(JavaCodeChunkerService.class);
    private final JavaParser javaParser = new JavaParser();
    private final FileFinder fileFinder;
    private final MetricsCalculator metricsCalculator;
    private final ObjectMapper objectMapper;
    private final ApplicationProperties properties;

    /**
     * Analyzes project and generates one JSON file per class (class-level analysis)
     * Each file contains only class information
     */
    public AnalysisSummary generateClassFiles(String projectPath,
                                              List<String> includePatterns,
                                              List<String> excludePatterns,
                                              String outputDir) throws IOException {

        logger.info("Generating class files for project: {}", projectPath);
        logger.info("Include patterns: {}", includePatterns);
        logger.info("Exclude patterns: {}", excludePatterns);
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

        AnalysisSummary summary = new AnalysisSummary();
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
                    List<ChunkData> classChunks = extractClassChunksFromFile(cu, javaFile);

                    // Save each class to individual JSON file
                    for (ChunkData classChunk : classChunks) {
                        saveClassChunkToJson(classChunk, outputDir);
                        totalClasses++;

                        summary.addClassFile(
                                generateFileName(classChunk.getChunkId(),"class"),
                                classChunk.getChunkId(),
                                classChunk.getKind().name(),
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
    public AnalysisSummary generateMethodFiles(String projectPath,
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

        AnalysisSummary summary = new AnalysisSummary();
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
                    List<ClassInfo> classMethodsList = extractMethodsFromFile(cu, javaFile);

                    // Save each class's methods to individual JSON file
                    for (ClassInfo classMethods : classMethodsList) {
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
    private List<ChunkData> extractClassChunksFromFile(CompilationUnit cu, Path filePath) {
        List<ChunkData> classChunks = new ArrayList<>();
        String packageName = cu.getPackageDeclaration()
                .map(p -> p.getNameAsString())
                .orElse("");
        logger.info("***packageName: {}", packageName);

        cu.accept(new VoidVisitorAdapter<Void>() {
            @Override
            public void visit(ClassOrInterfaceDeclaration n, Void arg) {
                ChunkData chunk = createClassChunk(n, filePath, packageName,
                        n.isInterface() ? "INTERFACE" : "CLASS");
                classChunks.add(chunk);
                super.visit(n, arg);
            }

            @Override
            public void visit(EnumDeclaration n, Void arg) {
                ChunkData chunk = createClassChunk(n, filePath, packageName, "ENUM");
                classChunks.add(chunk);
                super.visit(n, arg);
            }

            @Override
            public void visit(AnnotationDeclaration n, Void arg) {
                ChunkData chunk = createClassChunk(n, filePath, packageName, "ANNOTATION");
                classChunks.add(chunk);
                super.visit(n, arg);
            }
        }, null);

        SymbolsExtractor.getClassSymbols(cu, classChunks.get(0).getSymbols());

        return classChunks;
    }

    /**
     * Extracts methods from a file, grouped by class
     */
    private List<ClassInfo> extractMethodsFromFile(CompilationUnit cu, Path filePath) {
        List<ClassInfo> classMethodsList = new ArrayList<>();
        String packageName = cu.getPackageDeclaration()
                .map(p -> p.getNameAsString())
                .orElse("");

        Map<String, List<ChunkData>> methodsByClass = new HashMap<>();

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
            ClassInfo classMethods = findOrCreateClassMethods(
                    classMethodsList, fullyQualifiedName, className, packageName, type, filePath);

            // Create method chunk
            ChunkData chunkData = createMethodChunkData(method, filePath, packageName);
            classMethods.getMethods().add(chunkData);
        }

        // Find all methods and group them by containing class
        List<ConstructorDeclaration> allConstructors = cu.findAll(ConstructorDeclaration.class);
        for (ConstructorDeclaration constructor : allConstructors) {
            // Find containing class
            Optional<ClassOrInterfaceDeclaration> containingClass =
                    constructor.findAncestor(ClassOrInterfaceDeclaration.class);

            String className = containingClass.map(ClassOrInterfaceDeclaration::getNameAsString).orElse("");
            String fullyQualifiedName = packageName.isEmpty() ?
                    className : packageName + "." + className;

            String type = "CLASS";
            if (containingClass.isPresent()) {
                type = containingClass.get().isInterface() ? "INTERFACE" : "CLASS";
            }

            // Get or create class methods entry
            ClassInfo classMethods = findOrCreateClassMethods(
                    classMethodsList, fullyQualifiedName, className, packageName, type, filePath);

            // Create method chunk
            ChunkData methodChunk = createMethodChunkData(constructor, filePath, packageName);
            classMethods.getMethods().add(methodChunk);
        }

        // Also handle methods in enums
        List<EnumDeclaration> enums = cu.findAll(EnumDeclaration.class);
        for (EnumDeclaration enumDecl : enums) {
            String className = enumDecl.getNameAsString();
            String fullyQualifiedName = packageName.isEmpty() ?
                    className : packageName + "." + className;

            ClassInfo classMethods = findOrCreateClassMethods(
                    classMethodsList, fullyQualifiedName, className, packageName, "ENUM", filePath);

            for (MethodDeclaration methodDeclaration : enumDecl.getMethods()) {
                ChunkData chunkData = createMethodChunkData(methodDeclaration, filePath, packageName);
                classMethods.getMethods().add(chunkData);
            }

            for (ConstructorDeclaration constructorDeclaration : enumDecl.getConstructors()) {
                ChunkData chunkData = createMethodChunkData(constructorDeclaration, filePath, packageName);
                classMethods.getMethods().add(chunkData);
            }
        }

        return classMethodsList;
    }

    /**
     * Finds or creates a ClassMethods entry
     */
    private ClassInfo findOrCreateClassMethods(List<ClassInfo> classMethodsList,
                                               String fullyQualifiedName,
                                               String className,
                                               String packageName,
                                               String type,
                                               Path filePath) {
        for (ClassInfo cm : classMethodsList) {
            if (cm.getFullyQualifiedName().equals(fullyQualifiedName)) {
                return cm;
            }
        }

        ClassInfo newClassMethods = ClassInfo.builder()
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
    private ChunkData createClassChunk(TypeDeclaration<?> typeDecl,
                                       Path filePath,
                                       String packageName,
                                       String type) {
        String className = typeDecl.getNameAsString();

        ChunkData.ChunkDataBuilder builder = ChunkData.builder();

        builder.fullyQualifiedName(packageName.isEmpty() ? className : packageName + "." + className);

        // #1.language - value["java"]
        builder.language("java");

        // #2.filePath - full path to the analyzed java class file
        builder.filePath(filePath.toAbsolutePath().toString());

        // #3.chunkId - full package path of the analyzed java class
        builder.chunkId(packageName.isEmpty() ? className : packageName + "." + className);

        // #4.kind - value["CLASS"]
        builder.kind(Kind.CLASS);

        // #5.name - get class name of the java class
        builder.name(className);

        // #6.parent - to extract parent classes and interfaces
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

        // #7.signature - to be enhanced (no direct way to extract class signature
        if (typeDecl instanceof ClassOrInterfaceDeclaration) {
            ClassOrInterfaceDeclaration classDecl = (ClassOrInterfaceDeclaration) typeDecl;

            builder.signature(SignatureExtractor.getClassSignature(classDecl));
        }

        // #8.location - to get the start line and end line of the class
        typeDecl.getRange().ifPresent(range -> {
            builder.location(new Location(range.begin.line, range.end.line));
        });

        // #9.imports - to get all the import statements of the java class
        List<String> dependencies = new ArrayList<>();

        typeDecl.findCompilationUnit().ifPresent(cu -> {
            for (com.github.javaparser.ast.ImportDeclaration imp : cu.getImports()) {
                dependencies.add(imp.getNameAsString());
            }
        });

        builder.imports(dependencies);

        // #10.modifiers - to get the modifider of the class
        List<String> modifiers = new ArrayList<>();

        for (com.github.javaparser.ast.Modifier mod : typeDecl.getModifiers()) {
            modifiers.add(mod.toString());
        }

        builder.modifiers(modifiers);

        // #11.symbols - to be enhanced (no direct way to extract those info
        Symbols.SymbolsBuilder symbolsBuilder = Symbols.builder();
        symbolsBuilder.classes(new ArrayList<>());
        symbolsBuilder.methods(new ArrayList<>());
        symbolsBuilder.fields(new ArrayList<>());
        symbolsBuilder.variables(new ArrayList<>());

        builder.symbols(symbolsBuilder.build());

        // #12.code - to extract the code of the java class
        String code = typeDecl.toString();

        int maxSnippetLength = properties.getChunk().getMaxSnippetLength();
        logger.debug("maxSnippetLength = {}", maxSnippetLength);
        logger.debug("code length = {}", code.length());

        builder.code(code);

        // #13.notes - to add the notes
        Notes.NotesBuilder notesBuilder = Notes.builder();
        notesBuilder.missingData(null);
        notesBuilder.extractionWarnings(null);

        builder.notes(notesBuilder.build());

        return builder.build();
    }

    /**
     * Creates a MethodChunk from MethodDeclaration
     */
    private ChunkData createMethodChunkData(Node node, Path filePath, String packageName) {
        ChunkData.ChunkDataBuilder builder = ChunkData.builder();

        if(node instanceof MethodDeclaration){
            MethodDeclaration methodDeclaration = (MethodDeclaration) node;
            String methodName = methodDeclaration.getNameAsString();

            // Find containing class
            ClassOrInterfaceDeclaration containingClass = methodDeclaration.findAncestor(ClassOrInterfaceDeclaration.class).get();
            String className = containingClass.getNameAsString();

            builder.fullyQualifiedName(packageName.isEmpty() ? className + "." + methodName : packageName + "." + className + "." + methodName);

            // #1.language - value["java"]
            builder.language("java");

            // #2.filePath - full path to the analyzed java class file
            builder.filePath(filePath.toAbsolutePath().toString());

            // #3.chunkId - full package path of the analyzed java class
            builder.chunkId(packageName.isEmpty() ? className : packageName + "." + className);

            // #4.kind - value["METHOD"]
            builder.kind(Kind.METHOD);

            // #5.name - get class name of the java class
            builder.name(className);

            // #6.parent - to extract parent classes and interfaces
            List<String> extendedClasses = new ArrayList<>();
            for (com.github.javaparser.ast.type.ClassOrInterfaceType ext : containingClass.getExtendedTypes()) {
                extendedClasses.add(ext.getNameAsString());
            }

            List<String> implementedInterfaces = new ArrayList<>();
            for (com.github.javaparser.ast.type.ClassOrInterfaceType imp : containingClass.getImplementedTypes()) {
                implementedInterfaces.add(imp.getNameAsString());
            }

            builder.parent(new ParentRef(packageName,
                    Stream.concat(extendedClasses.stream(), implementedInterfaces.stream()).collect(Collectors.toList())));

            // #7.signature - to be enhanced (no direct way to extract class signature
            builder.signature(SignatureExtractor.getMethodSignature(methodDeclaration));

            // #8.location - to get the start line and end line of the class
            methodDeclaration.getRange().ifPresent(range -> {
                builder.location(new Location(range.begin.line, range.end.line));
            });

            // #9.imports - to get all the import statements of the java class
            List<String> dependencies = new ArrayList<>();

            methodDeclaration.findCompilationUnit().ifPresent(cu -> {
                for (com.github.javaparser.ast.ImportDeclaration imp : cu.getImports()) {
                    dependencies.add(imp.getNameAsString());
                }
            });

            builder.imports(dependencies);

            // #10.modifiers - to get the modifider of the class
            List<String> modifiers = new ArrayList<>();

            for (com.github.javaparser.ast.Modifier mod : methodDeclaration.getModifiers()) {
                modifiers.add(mod.toString());
            }

            builder.modifiers(modifiers);

            // #11.symbols - to be enhanced (no direct way to extract those info
            Symbols.SymbolsBuilder symbolsBuilder = Symbols.builder();
            symbolsBuilder.classes(new ArrayList<>());
            symbolsBuilder.methods(new ArrayList<>());
            symbolsBuilder.fields(new ArrayList<>());
            symbolsBuilder.variables(new ArrayList<>());

            builder.symbols(symbolsBuilder.build());

            // #12.code - to extract the code of the java class
            String code = methodDeclaration.toString();

            int maxSnippetLength = properties.getChunk().getMaxSnippetLength();
            logger.debug("maxSnippetLength = {}", maxSnippetLength);
            logger.debug("code length = {}", code.length());

            builder.code(code);

            // #13.notes - to add the notes
            Notes.NotesBuilder notesBuilder = Notes.builder();
            notesBuilder.missingData(null);
            notesBuilder.extractionWarnings(null);

            builder.notes(notesBuilder.build());
        }  else if(node instanceof ConstructorDeclaration){
            ConstructorDeclaration constructorDeclaration = (ConstructorDeclaration) node;
            String methodName = constructorDeclaration.getNameAsString();

            // Find containing class
            ClassOrInterfaceDeclaration containingClass = constructorDeclaration.findAncestor(ClassOrInterfaceDeclaration.class).get();
            String className = containingClass.getNameAsString();


            builder.fullyQualifiedName(packageName.isEmpty() ? className + "." + methodName : packageName + "." + className + "." + methodName);

            // #1.language - value["java"]
            builder.language("java");

            // #2.filePath - full path to the analyzed java class file
            builder.filePath(filePath.toAbsolutePath().toString());

            // #3.chunkId - full package path of the analyzed java class
            builder.chunkId(packageName.isEmpty() ? className : packageName + "." + className);

            // #4.kind - value["METHOD"]
            builder.kind(Kind.METHOD);

            // #5.name - get class name of the java class
            builder.name(className);

            // #6.parent - to extract parent classes and interfaces
            List<String> extendedClasses = new ArrayList<>();
            for (com.github.javaparser.ast.type.ClassOrInterfaceType ext : containingClass.getExtendedTypes()) {
                extendedClasses.add(ext.getNameAsString());
            }

            List<String> implementedInterfaces = new ArrayList<>();
            for (com.github.javaparser.ast.type.ClassOrInterfaceType imp : containingClass.getImplementedTypes()) {
                implementedInterfaces.add(imp.getNameAsString());
            }

            builder.parent(new ParentRef(packageName,
                    Stream.concat(extendedClasses.stream(), implementedInterfaces.stream()).collect(Collectors.toList())));

            // #7.signature - to be enhanced (no direct way to extract class signature
            builder.signature(SignatureExtractor.getConstructorSignature(constructorDeclaration));

            // #8.location - to get the start line and end line of the class
            constructorDeclaration.getRange().ifPresent(range -> {
                builder.location(new Location(range.begin.line, range.end.line));
            });

            // #9.imports - to get all the import statements of the java class
            List<String> dependencies = new ArrayList<>();

            constructorDeclaration.findCompilationUnit().ifPresent(cu -> {
                for (com.github.javaparser.ast.ImportDeclaration imp : cu.getImports()) {
                    dependencies.add(imp.getNameAsString());
                }
            });

            builder.imports(dependencies);

            // #10.modifiers - to get the modifider of the class
            List<String> modifiers = new ArrayList<>();

            for (com.github.javaparser.ast.Modifier mod : constructorDeclaration.getModifiers()) {
                modifiers.add(mod.toString());
            }

            builder.modifiers(modifiers);

            // #11.symbols - to be enhanced (no direct way to extract those info
            Symbols.SymbolsBuilder symbolsBuilder = Symbols.builder();
            symbolsBuilder.classes(new ArrayList<>());
            symbolsBuilder.methods(new ArrayList<>());
            symbolsBuilder.fields(new ArrayList<>());
            symbolsBuilder.variables(new ArrayList<>());

            builder.symbols(symbolsBuilder.build());

            // #12.code - to extract the code of the java class
            String code = constructorDeclaration.toString();

            int maxSnippetLength = properties.getChunk().getMaxSnippetLength();
            logger.debug("maxSnippetLength = {}", maxSnippetLength);
            logger.debug("code length = {}", code.length());

            builder.code(code);

            // #13.notes - to add the notes
            Notes.NotesBuilder notesBuilder = Notes.builder();
            notesBuilder.missingData(null);
            notesBuilder.extractionWarnings(null);

            builder.notes(notesBuilder.build());
        }

        return builder.build();
    }

    /**
     * Saves a single class chunk to JSON file (class-level output)
     */
    private void saveClassChunkToJson(ChunkData classChunk, String outputDir) throws IOException {
        String fileName = generateFileName(classChunk.getFullyQualifiedName(), "class");

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
    private void saveMethodsToJson(ClassInfo classMethods, String outputDir) throws IOException {
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
    private void saveProjectSummary(AnalysisSummary summary, String outputDir) throws IOException {
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
}