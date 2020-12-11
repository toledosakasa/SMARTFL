package trace;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

public class CallTest {
    public static boolean bar(int a, int b){    
        return a == b;
    }

    public static boolean foo(int a, int b) {
        a = a+1;
        b = b+2;//b+3
        return bar(a, b);
	}

	@Test
	void pass() {
		assertEquals(foo(1,10), false);
	}

	@Test
	void fail() {
		assertEquals(foo(2,0), true);
	}
}