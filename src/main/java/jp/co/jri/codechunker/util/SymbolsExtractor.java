package jp.co.jri.codechunker.util;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;

import jp.co.jri.codechunker.model.chunk.data.Symbols;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SymbolsExtractor {
    private static final Logger logger = LoggerFactory.getLogger(SymbolsExtractor.class);

    public static void getClassSymbols(CompilationUnit cu, Symbols symbols) {
        try {
            SymbolsVisitor visitor = new SymbolsVisitor(symbols);
            cu.accept(visitor, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Simple visitor that only collects names
    static class SymbolsVisitor extends VoidVisitorAdapter<Void> {
        private Symbols symbols;
        private String currentMethodName = "";

        public SymbolsVisitor(Symbols symbols) {
            this.symbols = symbols;
        }

        @Override
        public void visit(ClassOrInterfaceDeclaration n, Void arg) {
            // Add class/interface name
            symbols.getClasses().add(n.getNameAsString());
            super.visit(n, arg);
        }

        @Override
        public void visit(MethodDeclaration n, Void arg) {
            // Add method name
            symbols.getMethods().add(n.getNameAsString());

            // Track current method for variable context
            String previousMethodName = currentMethodName;
            currentMethodName = n.getNameAsString();

            super.visit(n, arg);

            // Restore context
            currentMethodName = previousMethodName;
        }

        @Override
        public void visit(ConstructorDeclaration n, Void arg) {
            // Add constructor name (same as class name)
            symbols.getMethods().add(n.getNameAsString());

            // Track current method for variable context
            String previousMethodName = currentMethodName;
            currentMethodName = n.getNameAsString();

            super.visit(n, arg);

            // Restore context
            currentMethodName = previousMethodName;
        }

        @Override
        public void visit(FieldDeclaration n, Void arg) {
            // Add all field names from this declaration
            for (VariableDeclarator var : n.getVariables()) {
                symbols.getFields().add(var.getNameAsString());
            }
            super.visit(n, arg);
        }

        @Override
        public void visit(com.github.javaparser.ast.expr.VariableDeclarationExpr n, Void arg) {
            // Add local variable names (only if we're inside a method)
            if (!currentMethodName.isEmpty()) {
                for (VariableDeclarator var : n.getVariables()) {
                    symbols.getVariables().add(var.getNameAsString());
                }
            }
            super.visit(n, arg);
        }
    }
}
