package trace;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class IfTest {

	public static int f(int a) {
        if (a >0)
        {
            a = a + 1;
            a = a + 2;   
        }
		return a;
	}

	@Test
	void pass() {
		assertEquals(f(2), 5);
	}

	@Test
	void fail() {
		assertEquals(f(1), 1);
	}

}
