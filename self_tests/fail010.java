class Test {
    int foo;
    boolean bar;
    
    private void dummyMethod() {
        
    }
    
    Test baz(int foo, Test baz, boolean pass) {
        foo = this;
        baz = 18;
        pass = false; // Should not error
        return null;
    }
}

class Dummy {
    
}