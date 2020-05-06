package trace;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class gcdtest {

	public static int gcd(int a, int b) {
		int r = 0;
		while (b > 0) {
			r = a % b;
			a = b;
			b = r;
		}
		a = a + 1;

		return a;
	}

	@Test
	void test() {
		gcd(28, 8);
	}

}
