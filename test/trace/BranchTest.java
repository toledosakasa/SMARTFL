package trace;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class BranchTest {

	public static int f(int a) {
		if(a > 1)
			a = a + 1;
		else
			a = a - 1;
		return a;
	}

	@Test
	void pass() {
		assertEquals(f(2), 2);
	}

	@Test
	void fail() {
		assertEquals(f(3), 4);
	}

}
