package miniJava;

import static miniJava.SyntacticAnalyzer.Token.Kind.*;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import mJAM.Disassembler;
import mJAM.Interpreter;
import mJAM.ObjectFile;
import miniJava.AbstractSyntaxTrees.AST;
import miniJava.AbstractSyntaxTrees.ASTDisplay;
import miniJava.CodeGenerator.CodeGenerator;
import miniJava.ContextualAnalyzer.ContextualAnalyzer;
import miniJava.SyntacticAnalyzer.Parser;
import miniJava.SyntacticAnalyzer.Scanner;

public class Compiler {
    public enum RunMode {
        JUST_COMPILE, AUTO_RUN, AUTO_DEBUG
    }

    public static void main(String[] args) {
        RunMode mode;
        String path;

        switch (args.length) {
            case 0:
                throw new IllegalArgumentException("No file path to compile provided");
            case 1:
                mode = RunMode.JUST_COMPILE;
                path = args[0];
                break;
            default:
                if (args[0].equals("-r") || args[0].equals("--run")) {
                    mode = RunMode.AUTO_RUN;
                    path = args[1];
                } else if (args[0].equals("-d") || args[0].equals("--debug")) {
                    mode = RunMode.AUTO_DEBUG;
                    path = args[1];
                } else {
                    if (args[1].equals("-r") || args[1].equals("--run")) {
                        mode = RunMode.AUTO_RUN;
                        path = args[0];
                    } else if (args[1].equals("-d") || args[1].equals("--debug")) {
                        mode = RunMode.AUTO_DEBUG;
                        path = args[0];
                    } else {
                        throw new IllegalArgumentException("-r/--run and -d/--debug are the only "
                                + "accepted flags, and they cannot be passed together");
                    }
                }
        }

        System.exit(runAllOnFile(path, false, mode));
    }

    public static int runAllOnFile(String path, boolean displayTree, RunMode autoRunAndDebug) {
        ASTDisplay.showPosition = false;
        ASTDisplay.showTypes = false;

        InputStream iStream;
        try {
            iStream = new FileInputStream(path);

            return runFullCompiler(iStream, path, displayTree, autoRunAndDebug);

        } catch (FileNotFoundException e) {
            System.err.printf("Attempted to open %s, but file could not be read. "
                    + "See the following stack trace for details.%n", path);
            e.printStackTrace();
            return 1;
        }
    }

    public static int runThroughCAOnFile(String path, boolean displayTree) {
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

    public static int runFullCompiler(InputStream iStream, String inputPath, boolean displayTree,
            RunMode autoRunAndDebug) {
        // Run the parser & contextual analysis first
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

        // Run the code generator
        CodeGenerator.generateCode(ast);

        // Write the object file
        String objectCodeFileName = inputPath.substring(0, inputPath.length() - 4) + "mJAM";
        ObjectFile objF = new ObjectFile(objectCodeFileName);
        System.out.print("Writing object code file " + objectCodeFileName + " ... ");
        if (objF.write()) {
            System.out.println("FAILED!");
            return -1;
        } else {
            System.out.println("SUCCEEDED");
        }

        // Under normal operation, we're done at this point, but for testing, we have the
        // option to automatically run or debug
        if (autoRunAndDebug == RunMode.AUTO_RUN) {
            Interpreter.interpret(objectCodeFileName);

        } else if (autoRunAndDebug == RunMode.AUTO_DEBUG) {
            // Create asm file corresponding to object code using disassembler 
            String asmCodeFileName = objectCodeFileName.substring(0, inputPath.length() - 4)
                    + "asm";
            System.out.print("Writing assembly file " + asmCodeFileName + " ... ");
            Disassembler d = new Disassembler(objectCodeFileName);
            if (d.disassemble()) {
                System.out.println("FAILED!");
                return -1;
            } else System.out.println("SUCCEEDED");

            // Run the debugger
            System.out.println("Running code in debugger ... ");
            Interpreter.debug(objectCodeFileName, asmCodeFileName);

            System.out.println("*** mJAM execution completed");
        }

        // If we've reached this point, there weren't any compilation errors, so we can exit with 0
        return 0;
    }

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
