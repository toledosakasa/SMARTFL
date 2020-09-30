package trace;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class SimpleFlowTest {

	public static int f(int a) {
		a = a + 1;
		a = a + 1;
		a = a + 1;
		a = a + 1;
		a = a + 1;
		a = a + 1;
		a = a + 1;
		a = a + 1;
		return a;
	}

	@Test
	void fail() {
		assertEquals(f(3), 4);
	}

}
