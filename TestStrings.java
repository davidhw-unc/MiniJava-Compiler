class TestStrings { // This is a line comment
    public int int1;
    public static Test1[] reference;

    private void hello(int i, Foo bar) {
        bar.t1 = i < i + 1;
        // In order: carriage return, backspace, tab, newline, form feed
        String testString = "\r\b\t\n\f\"\'\\"; // Not yet incorporating Unicode literals
        /* This is a block comment
        bar.t2 = */ bar.t2 = i >= i + 13;
    }
}

class Example {
    void bar() {
    }

    void foo2() {
        if (true) {
            return;
        } else if (false) {
            return "Hello!";
        } else {
            return;
        }
    }

    void foo(int state) {
        if (true) return;
        if (true)
            return;
        else if (false)
            return;
        else return;
    }
}

class WrapExample {
    void bar2() {
        while (!stop)
            doSomething();
    }
}
