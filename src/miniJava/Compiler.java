package miniJava;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import miniJava.SyntacticAnalyzer.Parser;
import miniJava.SyntacticAnalyzer.Scanner;
import static miniJava.SyntacticAnalyzer.Token.Kind.*;

public class Compiler {
    public static void main(String[] args) {
        InputStream iStream;
        try {
            iStream = new FileInputStream(args[0]);
            ErrorReporter reporter = new ErrorReporter();

            testScanner(iStream, reporter);
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
    private static void checkProgram(InputStream iStream, ErrorReporter reporter) {
        new Parser(new Scanner(iStream, reporter), reporter).parse();
        if (reporter.hasErrors()) {
            System.out.println("INVALID miniJava program");
            // return code for invalid input
            System.exit(4);
        } else {
            System.out.println("valid miniJava program");
            // return code for valid input
            System.exit(0);
        }
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
