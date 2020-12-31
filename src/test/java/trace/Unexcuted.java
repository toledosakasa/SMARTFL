package trace;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

public class Unexcuted {
    public static int foo(int a, int b) {
        int c = 0;
        int d = 0;
        if (a < 5) {
            a += 10;
            // b=b;
            // c++;
            // b+=100;
        } else {
            b += 10;
            // a=a;
            // d++;
        }
        int r = b * b;
        return r;
    }

    @Test
    void pass() {
        assertEquals(foo(10, 0), 100);
    }

    @Test
    void fail() {
        assertEquals(foo(0, 0), 10000);
    }
}