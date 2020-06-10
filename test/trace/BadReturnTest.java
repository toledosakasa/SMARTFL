package trace;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class BadReturnTest {

	public int fact(int n) {
		if (n <= 0)
			return 1;
		if (n == 2)
			return 300000;// wrong
		int ret = fact(n - 1);
		return n * ret;
	}

	@Test
	void pass() {
		assertEquals(fact(0), 1);
	}

	@Test
	void fail() {
		assertEquals(fact(3), 6);
	}

}
