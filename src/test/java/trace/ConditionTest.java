package trace;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

public class ConditionTest {
    public static int foo(int a, int b) {
        int rst = 0;
        if (a > 2) {
            rst++;
        } else if (a < -1) { // should be a < -2
            rst -= 10;
        }
        if (b > 3) {
            rst += 100;
        } else if (b < -3) {
            rst -= 1000;
        }
        return rst;
    }

    @Test
    void pass1() {
        assertEquals(foo(3, 2), 1);
    }

    @Test
    void pass2() {
        assertEquals(foo(-5, -5), -1010);
    }

    @Test
    void fail1() {
        assertEquals(foo(-2, 4), 100);
    }
    // void fail2() {
    // assertEquals(foo(-2,4), 100);
    // }
}