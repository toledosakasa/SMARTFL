package trace;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class BreakTest {

	public int f(int n) {
		int sum = 0;
		for (int i = 0; i < n; i++) {
			if (sum > 30)
				break;
			sum = sum + i;
		}
		return sum;
	}

	@Test
	void pass() {
		assertEquals(0, f(0));
	}

	@Test
	void fail() {
		assertEquals(55, f(11));
	}

}
