package trace;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class MergeTest {

	public static int f(int a) {
		if (a > 1)
			a = a + 1;
		a = a + 2;
		return a;
	}

	@Test
	void pass() {
		assertEquals(f(1), 3);
	}

	@Test
	void fail() {
		assertEquals(f(2), 4);
	}

}
