package miniJava;

import static miniJava.SyntacticAnalyzer.Token.Kind.*;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import miniJava.AbstractSyntaxTrees.AST;
import miniJava.AbstractSyntaxTrees.ASTDisplay;
import miniJava.AbstractSyntaxTrees.ContextualAnalyzer;
import miniJava.SyntacticAnalyzer.Parser;
import miniJava.SyntacticAnalyzer.Scanner;

public class Compiler {
    public static void main(String[] args) {
        System.exit(runOnFile(args[0], false));
    }

    public static int runOnFile(String path, boolean displayTree) {
        ASTDisplay.showPosition = false;
        ASTDisplay.showTypes = false;

        InputStream iStream;
        try {
            iStream = new FileInputStream(path);

            return runParserWithContextualAnalysis(iStream, displayTree);

        } catch (FileNotFoundException e) {
            System.err.printf("Attempted to open %s, but file could not be read. "
                    + "See the following stack trace for details.%n", path);
            e.printStackTrace();
            return 1;
        }
    }

    // These methods each return the intended exit code
    // (Done this way for testing purposes)

    public static int runParserWithContextualAnalysis(InputStream iStream, boolean displayTree) {
        ErrorReporter reporter = new ErrorReporter();
        Parser parser = new Parser(new Scanner(iStream, reporter), reporter);
        AST ast = parser.parse();
        if (!reporter.hasErrors()) {
            ContextualAnalyzer.runAnalysis(ast, reporter);
        }

        if (reporter.hasErrors()) {
            System.out.println("INVALID miniJava program");
            return 4;
        }

        System.out.println("valid miniJava program");
        if (displayTree) {
            ASTDisplay display = new ASTDisplay();
            display.showTree(ast);
        }
        return 0;
    }

    @SuppressWarnings("unused")
    private static int runParserAlone(InputStream iStream) {
        ErrorReporter reporter = new ErrorReporter();
        Parser parser = new Parser(new Scanner(iStream, reporter), reporter);
        AST ast = parser.parse();

        if (reporter.hasErrors()) {
            System.out.println("INVALID miniJava program");
            return 4;
        }

        System.out.println("valid miniJava program");
        ASTDisplay display = new ASTDisplay();
        display.showTree(ast);
        return 0;
    }

    @SuppressWarnings("unused")
    private static void testScanner(InputStream iStream, ErrorReporter reporter) {
        Scanner scanner = new Scanner(iStream, reporter);
        while (scanner.peek().kind != EOT) {
            System.out.println(scanner.peek());
            scanner.pop();
        }
    }
}
