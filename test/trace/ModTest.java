package trace;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ModTest {

	public static int f(int a) {
		a = a % 2;//should be a % 4
		a = a + 1;
		return a;
	}

	@Test
	void pass() {
		assertEquals(f(1), 2);
	}

	@Test
	void fail() {
		assertEquals(f(3), 4);
	}

}
