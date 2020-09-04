package trace;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class gcdtest {

	public static int gcd(int a, int b) {
        int r = 0;
        int c = 5;
        int e = c + 3;
        int d = c + 5;
        d = d + e;
		while (b > 0) {
			r = a % b;
			a = b;
			b = r;
		}
        r = r + 1;
        r = r + d;
        a = a + d;
		return a;
	}

	@Test
	void test() {
		gcd(28, 8);
	}

}
