class Test {
    int foo;
    boolean bar;
    
    private void dummyMethod() {
        return 17;
    }
    
    Test baz(int foo, Test boz, boolean pass) {
        return;
        return 6;
    }
}

class Dummy {
    void test() {
        if (17) {
            int i = -false;
            int j = false - 17;
        } else {
            int i = 0;
        }
    }
}