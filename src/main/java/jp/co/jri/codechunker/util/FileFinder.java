package jp.co.jri.codechunker.util;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class FileFinder {

    private static final Logger logger = LoggerFactory.getLogger(FileFinder.class);

    public List<Path> findJavaFiles(String projectPath,
                                    List<String> includePatterns,
                                    List<String> excludePatterns) throws IOException {

        List<Path> javaFiles = new ArrayList<>();
        Path startPath = Paths.get(projectPath);

        if (!Files.exists(startPath)) {
            throw new IOException("Project path does not exist: " + projectPath);
        }

        if (!Files.isDirectory(startPath)) {
            throw new IOException("Project path is not a directory: " + projectPath);
        }

        logger.debug("Searching for Java files in: {}", startPath.toAbsolutePath());

        Files.walkFileTree(startPath, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                String fileName = file.toString();

                if (fileName.endsWith(".java")) {
                    boolean include = true;

                    if (includePatterns != null && !includePatterns.isEmpty()) {
                        include = false;
                        for (String pattern : includePatterns) {
                            if (fileName.matches(pattern)) {
                                include = true;
                                break;
                            }
                        }
                    }

                    if (include && excludePatterns != null) {
                        for (String pattern : excludePatterns) {
                            if (fileName.matches(pattern)) {
                                include = false;
                                break;
                            }
                        }
                    }

                    if (include) {
                        javaFiles.add(file);
                    }
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                String dirName = dir.getFileName().toString();
                if (dirName.equals("target") || dirName.equals("bin") ||
                        dirName.equals(".git") || dirName.equals("build") ||
                        dirName.equals("node_modules") || dirName.equals(".idea") ||
                        dirName.equals(".vscode")) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                return FileVisitResult.CONTINUE;
            }
        });

        logger.info("Found {} Java files to analyze", javaFiles.size());
        return javaFiles;
    }
}
