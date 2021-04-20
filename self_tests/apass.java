class Test1 {
    public static void main(String[] args) {
        Helper help = new Helper();
        help.initialize(5);
        
        int min = -20;
        int max = 20;

        int i = min;
        while (i <= max) {
            help.input(i);
            System.out.println(i);
            System.out.println(help.getValue());
            i = i + 1;
        }
        
        i = min;
        while (i <= max) {
            System.out.println(i);
            System.out.println(Test1.fibonacci(i));
            i = i + 1;
        }
        
        // Random stuff for testing edge cases
        
        if (!(true == false)) {
            System.out.println(42);
        }
    }
    
    // More random testing
    void testCrap() {
        Test1 test = this;
    }
    
    // Returns the nth fibonacci number (starting 0, 1, 1)
    // CAN do negatives
    public static int fibonacci(int n) {
        if (n == 0) {
            return 0;
        }
        
        if (n == 1) {
            return 1;
        }
        
        if (n < 1) {
            return fibonacci(n + 2) - fibonacci(n + 1);
        }
        
        return fibonacci(n - 1) + fibonacci(n - 2);
    }
}

class Helper {
    private int lastValue;
    private int[] cache;
    private int curCapacity;
    
    public void initialize(int initialCapacity) {
        cache = null;
        cache = new int[initialCapacity];
        curCapacity = initialCapacity;
        cache[0] = 0;
        cache[1] = 1;
    }
    
    private void resize() {
        int[] newCache = new int[2 * curCapacity];
        int i = 0;
        while (i < curCapacity) {
            newCache[i] = cache[i];
            i=1+i;
        }
        curCapacity = 2*curCapacity;
        this.cache = newCache;
    }
    
    public void input(int n) {
        if (n < 0)
            this.lastValue = 0; 
        
        lastValue = calculate(n);
    }
    
    public int getValue() {
        return this.lastValue;
    }
    
    private int calculate(int n) {
        while (curCapacity <= n) {
            resize();
        }
        
        if (n != 0 && cache[n] == 0) {
            cache[n] = -(-calculate(n - 1) - calculate(n - 2));
        }
        
        return cache[n];
    }
    
    public int dummyTestMethod() {
        return cache.length;
    }
}