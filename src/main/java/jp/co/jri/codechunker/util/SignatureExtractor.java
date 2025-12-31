package jp.co.jri.codechunker.util;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
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

    public static String getMethodSignature(MethodDeclaration method) {
        StringBuilder signature = new StringBuilder();

        // Add modifiers (public, private, static, etc.)
        method.getModifiers().forEach(mod -> {
            signature.append(mod.toString().toLowerCase()).append(" ");
        });

        // Add type parameters if present
        if (!method.getTypeParameters().isEmpty()) {
            signature.append("<");
            for (int i = 0; i < method.getTypeParameters().size(); i++) {
                if (i > 0) signature.append(", ");
                signature.append(method.getTypeParameters().get(i).getNameAsString());
            }
            signature.append("> ");
        }

        // Add return type
        signature.append(method.getType()).append(" ");

        // Add method name
        signature.append(method.getNameAsString());

        // Add parameters
        signature.append("(");
        List<Parameter> parameters = method.getParameters();
        for (int i = 0; i < parameters.size(); i++) {
            if (i > 0) signature.append(", ");
            Parameter param = parameters.get(i);

            // Check if parameter is varargs
            if (param.isVarArgs()) {
                // Remove array brackets and add ...
                String type = param.getType().toString();
                type = type.replace("[]", "");
                signature.append(type).append("...");
            } else {
                signature.append(param.getType());
            }

            signature.append(" ").append(param.getNameAsString());
        }
        signature.append(")");

        // Add throws clause if present
        if (!method.getThrownExceptions().isEmpty()) {
            signature.append(" throws ");
            for (int i = 0; i < method.getThrownExceptions().size(); i++) {
                if (i > 0) signature.append(", ");
                signature.append(method.getThrownExceptions().get(i));
            }
        }

        return signature.toString();
    }

    public static String getConstructorSignature(ConstructorDeclaration constructor) {
        StringBuilder signature = new StringBuilder();

        // Add modifiers
        constructor.getModifiers().forEach(mod -> {
            signature.append(mod.toString().toLowerCase()).append(" ");
        });

        // Add constructor name
        signature.append(constructor.getNameAsString());

        // Add parameters
        signature.append("(");
        List<Parameter> parameters = constructor.getParameters();
        for (int i = 0; i < parameters.size(); i++) {
            if (i > 0) signature.append(", ");
            Parameter param = parameters.get(i);

            if (param.isVarArgs()) {
                String type = param.getType().toString().replace("[]", "");
                signature.append(type).append("...");
            } else {
                signature.append(param.getType());
            }

            signature.append(" ").append(param.getNameAsString());
        }
        signature.append(")");

        // Add throws clause if present
        if (!constructor.getThrownExceptions().isEmpty()) {
            signature.append(" throws ");
            for (int i = 0; i < constructor.getThrownExceptions().size(); i++) {
                if (i > 0) signature.append(", ");
                signature.append(constructor.getThrownExceptions().get(i));
            }
        }

        return signature.toString();
    }
}
