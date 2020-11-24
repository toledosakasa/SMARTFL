package trace;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class SwitchTest {
	public int f(int a) {
		switch (a) {
			case 0:
				a = a + 1;
			case 1:
				a = a + 2;// break;
			case 2:
				a = a + 3;
			case 3:
				a = a + 4;
				break;
			default:
				a = a < 0 ? a : -a;
		}
		return a;
	}

	@Test
	void pass() {
		assertEquals(f(2), 9);
		// assertEquals(f(2), 8);
	}

	@Test
	void fail() {
		assertEquals(f(0), 3);
		// assertEquals(f(1), 7);
	}
}
