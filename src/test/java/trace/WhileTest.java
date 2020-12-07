package trace;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

public class WhileTest {
    public static int foo(int a, int b) {
        int r = 0;
        while(a<10 
                && 
                a*b< 30){ // a*b<20
            r = r + a*b;
            a = a+1;
            b = b-1;
        }
        return r;
	}

	@Test
	void pass() {
		assertEquals(foo(9,2), 18);
	}

	@Test
	void fail() {
		assertEquals(foo(4,7), 0);
	}
}