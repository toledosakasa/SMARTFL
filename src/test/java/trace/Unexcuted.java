package trace;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

public class Unexcuted {
    public static int foo(int a, int b) {
        if(a<5)
        {
            a+=10;
            //b+=100;
        }
        else
        {
            b+=10;
        }
        int r = b*b;
        return r;
	}

	@Test
	void pass() {
		assertEquals(foo(10,0), 100);
	}

	@Test
	void fail() {
		assertEquals(foo(0,0), 10000);
	}
}