package miniJava;

public class ErrorReporter {

    private int numErrors;

    ErrorReporter() {
        numErrors = 0;
    }

    public boolean hasErrors() {
        return numErrors > 0;
    }

    public void reportError(String message) {
        System.err.println(message);
        reportError();
    }

    public void reportError() {
        numErrors++;
    }
}
