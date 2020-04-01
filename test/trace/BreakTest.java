package trace;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class BreakTest {

	public double f(int n) {
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
		assertEquals(f(0),0);
	}
	
	@Test
	void fail() {
		assertEquals(f(11),55);
	}

}
