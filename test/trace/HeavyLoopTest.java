package trace;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class HeavyLoopTest {

	public double pi(int n) {
		double ret = 0;
		double sum = 0;
		double all = 0;
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < n; j++) {
				if (i * i + j * j <= n * n) {
					sum = sum + 1;
				}
				all = all + 1;
			}
		}
		return sum / all * 4.0;
	}

	@Test
	void test() {
		pi(500);
	}

}
