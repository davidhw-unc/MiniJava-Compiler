package miniJava;

import static miniJava.SyntacticAnalyzer.Token.Kind.*;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import miniJava.AbstractSyntaxTrees.AST;
import miniJava.AbstractSyntaxTrees.ASTDisplay;
import miniJava.AbstractSyntaxTrees.TypeKind;
import miniJava.SyntacticAnalyzer.Parser;
import miniJava.SyntacticAnalyzer.Scanner;

public class Compiler {
    public static void main(String[] args) {
        InputStream iStream;
        try {
            iStream = new FileInputStream(args[0]);
            ErrorReporter reporter = new ErrorReporter();

            runParser(iStream, reporter);
            //testScanner(iStream, reporter);

        } catch (FileNotFoundException e) {
            System.err.printf(
                    "Attempted to open %s, but file could not be read. See the following stack trace for details.%n",
                    args[0]);
            e.printStackTrace();
            System.exit(1);
        }
    }

    @SuppressWarnings("unused")
    private static void runParser(InputStream iStream, ErrorReporter reporter) {
        Parser parser = new Parser(new Scanner(iStream, reporter), reporter);
        AST ast = parser.parse();

        if (reporter.hasErrors()) {
            System.out.println("INVALID miniJava program");
            System.exit(4);
        }

        System.out.println("valid miniJava program");
        ASTDisplay display = new ASTDisplay();
        display.showTree(ast);
        System.exit(0);
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
