package jp.co.jri.codechunker.util;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class SignatureExtractor {
    private static final Logger logger = LoggerFactory.getLogger(SignatureExtractor.class);

    public static String getClassSignature(ClassOrInterfaceDeclaration classDecl) {
        StringBuilder signature = new StringBuilder();

        // Get modifiers
        List<String> modifierList = new ArrayList<>();
        classDecl.getModifiers().forEach(mod -> {
            modifierList.add(mod.toString().trim());
        });

        if (!modifierList.isEmpty()) {
            signature.append(String.join(" ", modifierList)).append(" ");
        }

        // Add class or interface keyword
        if (classDecl.isInterface()) {
            signature.append("interface ");
        } else {
            signature.append("class ");
        }

        // Add class name
        signature.append(classDecl.getNameAsString());

        // Add type parameters
        if (classDecl.getTypeParameters() != null && !classDecl.getTypeParameters().isEmpty()) {
            signature.append("<");
            for (int i = 0; i < classDecl.getTypeParameters().size(); i++) {
                if (i > 0) signature.append(", ");
                signature.append(classDecl.getTypeParameters().get(i).getNameAsString());
            }
            signature.append(">");
        }

        // Add extends clause
        if (classDecl.getExtendedTypes() != null && !classDecl.getExtendedTypes().isEmpty()) {
            signature.append(" extends ");
            for (int i = 0; i < classDecl.getExtendedTypes().size(); i++) {
                if (i > 0) signature.append(", ");
                signature.append(classDecl.getExtendedTypes().get(i).getNameAsString());
            }
        }

        // Add implements clause
        if (classDecl.getImplementedTypes() != null && !classDecl.getImplementedTypes().isEmpty()) {
            signature.append(" implements ");
            for (int i = 0; i < classDecl.getImplementedTypes().size(); i++) {
                if (i > 0) signature.append(", ");
                signature.append(classDecl.getImplementedTypes().get(i).getNameAsString());
            }
        }

        return signature.toString();
    }
}
