package jp.co.jri.codechunker.service;

import jp.co.jri.codechunker.model.*;
import jp.co.jri.codechunker.model.chunk.ClassChunk;
import jp.co.jri.codechunker.model.chunk.Kind;
import jp.co.jri.codechunker.model.chunk.Location;
import jp.co.jri.codechunker.model.chunk.ParentRef;
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

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class JavaProjectChunkerService {

    private static final Logger logger = LoggerFactory.getLogger(JavaProjectChunkerService.class);
    private final JavaParser javaParser = new JavaParser();
    private final FileFinder fileFinder;
    private final MetricsCalculator metricsCalculator;
    private final ObjectMapper objectMapper;

    public ProjectAnalysisResult<ClassChunk> analyzeAtClassLevel(String projectPath,
                                                                 List<String> includePatterns,
                                                                 List<String> excludePatterns) throws IOException {
        logger.info("Starting class-level analysis for project: {}", projectPath);

        List<Path> javaFiles = fileFinder.findJavaFiles(projectPath, includePatterns, excludePatterns);

        List<ClassChunk> chunks = new ArrayList<>();
        int processedFiles = 0;
        int errorFiles = 0;
        int totalClasses = 0;

        for (Path javaFile : javaFiles) {
            logger.debug("Processing file: {}", javaFile);

            try {
                ParseResult<CompilationUnit> parseResult = javaParser.parse(javaFile);

                if (parseResult.isSuccessful() && parseResult.getResult().isPresent()) {
                    CompilationUnit cu = parseResult.getResult().get();
                    List<ClassChunk> fileChunks = analyzeFileAtClassLevel(cu, javaFile);
                    chunks.addAll(fileChunks);

                    totalClasses += cu.findAll(ClassOrInterfaceDeclaration.class).size();
                    totalClasses += cu.findAll(EnumDeclaration.class).size();
                    totalClasses += cu.findAll(AnnotationDeclaration.class).size();

                    processedFiles++;

                    if (processedFiles % 10 == 0) {
                        logger.info("Processed {} files...", processedFiles);
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

        ProjectAnalysisResult<ClassChunk> result = ProjectAnalysisResult.<ClassChunk>builder()
                .projectPath(projectPath)
                .analysisLevel("CLASS_LEVEL")
                .totalFiles(javaFiles.size())
                .totalClasses(totalClasses)
                .processedFiles(processedFiles)
                .errorFiles(errorFiles)
                .chunks(chunks)
                .build();

        logger.info("Class-level analysis completed: {} classes found in {} files",
                totalClasses, processedFiles);

        return result;
    }

    private List<ClassChunk> analyzeFileAtClassLevel(CompilationUnit cu, Path filePath) {
        List<ClassChunk> chunks = new ArrayList<>();
        String packageName = cu.getPackageDeclaration()
                .map(p -> p.getNameAsString())
                .orElse("");

        cu.accept(new VoidVisitorAdapter<Void>() {
            @Override
            public void visit(ClassOrInterfaceDeclaration n, Void arg) {
                String type = n.isInterface() ? "INTERFACE" : "CLASS";
                ClassChunk chunk = createClassChunk(n, filePath, packageName, type);
                chunks.add(chunk);
                super.visit(n, arg);
            }

            @Override
            public void visit(EnumDeclaration n, Void arg) {
                ClassChunk chunk = createClassChunk(n, filePath, packageName, "ENUM");
                chunks.add(chunk);
                super.visit(n, arg);
            }

            @Override
            public void visit(AnnotationDeclaration n, Void arg) {
                ClassChunk chunk = createClassChunk(n, filePath, packageName, "ANNOTATION");
                chunks.add(chunk);
                super.visit(n, arg);
            }
        }, null);

        return chunks;
    }

    private ClassChunk createClassChunk(TypeDeclaration<?> typeDecl, Path filePath,
                                        String packageName, String type) {
        String className = typeDecl.getNameAsString();

        ClassChunk.ClassChunkBuilder builder = ClassChunk.builder()
                .name(className)
                .kind(Kind.CLASS)
                .chunkId(packageName)
                .filePath(filePath.toAbsolutePath().toString());

        // Line information
        typeDecl.getRange().ifPresent(range -> {
//            builder.startLine(range.begin.line)
//                    .endLine(range.end.line)
//                    .totalLines(range.end.line - range.begin.line + 1);
            builder.location(new Location(range.begin.line, range.end.line));
        });

        // Modifiers
        List<String> modifiers = new ArrayList<>();
        typeDecl.getModifiers().forEach(mod -> modifiers.add(mod.toString()));
        builder.modifiers(modifiers);

        // Counts
//        builder.methodCount(typeDecl.getMethods().size())
//                .fieldCount(typeDecl.getFields().size());

        // Dependencies
        List<String> dependencies = new ArrayList<>();
        typeDecl.findCompilationUnit().ifPresent(cu ->
                cu.getImports().forEach(imp -> dependencies.add(imp.getNameAsString())));
        builder.imports(dependencies);

        // Inheritance
        if (typeDecl instanceof ClassOrInterfaceDeclaration) {
            ClassOrInterfaceDeclaration classDecl = (ClassOrInterfaceDeclaration) typeDecl;
            List<String> extendedClasses = new ArrayList<>();
            classDecl.getExtendedTypes().forEach(ext -> extendedClasses.add(ext.getNameAsString()));
//            builder.extendedClasses(extendedClasses);

            List<String> implementedInterfaces = new ArrayList<>();
            classDecl.getImplementedTypes().forEach(imp -> implementedInterfaces.add(imp.getNameAsString()));
//            builder.implementedInterfaces(implementedInterfaces);

            builder.parent(new ParentRef(packageName, Stream.concat(extendedClasses.stream(), implementedInterfaces.stream()).collect(Collectors.toList())));
        }

        // Code snippet
        String code = typeDecl.toString();
//        builder.codeSnippet(code.length() > 200 ? code.substring(0, 200) + "..." : code);
        builder.code(code);

        // Complexity
 //       builder.complexityScore(metricsCalculator.calculateComplexity(typeDecl));

        return builder.build();
    }

    public ProjectAnalysisResult<MethodChunk> analyzeAtMethodLevel(String projectPath,
                                                                   List<String> includePatterns,
                                                                   List<String> excludePatterns) throws IOException {
        logger.info("Starting method-level analysis for project: {}", projectPath);

        List<Path> javaFiles = fileFinder.findJavaFiles(projectPath, includePatterns, excludePatterns);

        List<MethodChunk> chunks = new ArrayList<>();
        int processedFiles = 0;
        int errorFiles = 0;
        int totalMethods = 0;

        for (Path javaFile : javaFiles) {
            logger.debug("Processing file: {}", javaFile);

            try {
                ParseResult<CompilationUnit> parseResult = javaParser.parse(javaFile);

                if (parseResult.isSuccessful() && parseResult.getResult().isPresent()) {
                    CompilationUnit cu = parseResult.getResult().get();
                    List<MethodChunk> fileChunks = analyzeFileAtMethodLevel(cu, javaFile);
                    chunks.addAll(fileChunks);

                    totalMethods += cu.findAll(MethodDeclaration.class).size();
                    processedFiles++;

                    if (processedFiles % 10 == 0) {
                        logger.info("Processed {} files...", processedFiles);
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

        ProjectAnalysisResult<MethodChunk> result = ProjectAnalysisResult.<MethodChunk>builder()
                .projectPath(projectPath)
                .analysisLevel("METHOD_LEVEL")
                .totalFiles(javaFiles.size())
                .totalMethods(totalMethods)
                .processedFiles(processedFiles)
                .errorFiles(errorFiles)
                .chunks(chunks)
                .build();

        logger.info("Method-level analysis completed: {} methods found in {} files",
                totalMethods, processedFiles);

        return result;
    }

    private List<MethodChunk> analyzeFileAtMethodLevel(CompilationUnit cu, Path filePath) {
        List<MethodChunk> chunks = new ArrayList<>();
        String packageName = cu.getPackageDeclaration()
                .map(p -> p.getNameAsString())
                .orElse("");

        List<MethodDeclaration> methods = cu.findAll(MethodDeclaration.class);

        for (MethodDeclaration method : methods) {
            MethodChunk chunk = createMethodChunk(method, filePath, packageName);
            chunks.add(chunk);
        }

        return chunks;
    }

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
        method.getModifiers().forEach(mod -> modifiers.add(mod.toString()));
        builder.modifiers(modifiers);

        // Parameters
        List<MethodChunk.Parameter> parameters = new ArrayList<>();
        method.getParameters().forEach(param -> {
            MethodChunk.Parameter paramObj = MethodChunk.Parameter.builder()
                    .name(param.getNameAsString())
                    .type(param.getType().toString())
                    .build();
            parameters.add(paramObj);
        });
        builder.parameters(parameters)
                .parameterCount(parameters.size());

        // Annotations
        List<String> annotations = new ArrayList<>();
        method.getAnnotations().forEach(ann -> annotations.add(ann.getNameAsString()));
        builder.annotations(annotations);

        // Throws declarations
        List<String> throwsList = new ArrayList<>();
        method.getThrownExceptions().forEach(ex -> throwsList.add(ex.toString()));
        builder.throwsDeclarations(throwsList);

        // Code metrics
        String methodBody = method.getBody().map(Object::toString).orElse("");
        builder.lineCount(methodBody.split("\r\n|\r|\n").length)
                .cyclomaticComplexity(metricsCalculator.calculateCyclomaticComplexity(method));

        // Method calls
        List<String> methodCalls = new ArrayList<>();
        method.findAll(com.github.javaparser.ast.expr.MethodCallExpr.class)
                .forEach(call -> methodCalls.add(call.getNameAsString()));
        builder.methodCalls(methodCalls);

        // Code snippet
        builder.codeSnippet(methodBody.length() > 150 ?
                methodBody.substring(0, 150) + "..." : methodBody);

        return builder.build();
    }

    private String generateChunkId(Path filePath, String name, String type) {
        String filename = filePath.getFileName().toString().replace(".java", "");
        return String.format("%s_%s_%s_%d",
                filename, name, type, System.currentTimeMillis());
    }

    public void saveReportToJson(Object report, String outputPath) throws IOException {
        logger.info("Saving report to: {}", outputPath);

        try (FileWriter writer = new FileWriter(outputPath)) {
            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(writer, report);
        }

        logger.debug("Report saved successfully");
    }
}