package trace;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

public class Unexecuted1 {
    public static int foo(int a, int b) {
        if(a<10){
            b ++;
        }
        //else b--;
        return b;
    }

    @Test
    void pass() {
        assertEquals(foo(5, 1), 2);
    }

    @Test
    void fail() {
        assertEquals(foo(20, 1), 0);
    }
}