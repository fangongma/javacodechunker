package jp.co.jri.codechunker.util;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import org.springframework.stereotype.Component;

@Component
public class MetricsCalculator {

    public int calculateComplexity(TypeDeclaration<?> typeDecl) {
        int complexity = 0;
        complexity += typeDecl.getMethods().size() * 2;
        complexity += typeDecl.findAll(com.github.javaparser.ast.body.ClassOrInterfaceDeclaration.class).size() * 3;
        complexity += typeDecl.getFields().size();
        return complexity;
    }

    public int calculateCyclomaticComplexity(MethodDeclaration method) {
        int complexity = 1;
        complexity += method.findAll(com.github.javaparser.ast.stmt.IfStmt.class).size();
        complexity += method.findAll(com.github.javaparser.ast.stmt.ForStmt.class).size();
        complexity += method.findAll(com.github.javaparser.ast.stmt.ForEachStmt.class).size();
        complexity += method.findAll(com.github.javaparser.ast.stmt.WhileStmt.class).size();
        complexity += method.findAll(com.github.javaparser.ast.stmt.DoStmt.class).size();

        // Use a mutable wrapper for the counter
        int[] switchCasesCount = {0};
        method.findAll(com.github.javaparser.ast.stmt.SwitchStmt.class)
                .forEach(switchStmt -> {
                    // Count the number of entries (cases) in the switch statement
                    switchCasesCount[0] += switchStmt.getEntries().size();
                });
        complexity += switchCasesCount[0];

        complexity += method.findAll(com.github.javaparser.ast.stmt.CatchClause.class).size();
        complexity += method.findAll(com.github.javaparser.ast.expr.ConditionalExpr.class).size();

        return complexity;
    }
}
