package trace;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class SwitchTest {
	public int f(int a) {
		switch (a) {// tableswitch
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
		switch (a) {// lookupswitch
			case 2:
				a = a + 1;
			case 34:
				a = a + 2;// break;
			case 56:
				a = a + 3;
			case 78:
				a = a + 4;
				break;
			default:
				a = a < 0 ? a : -a;
		}
		return a;
	}

	@Test
	void pass() {
		assertEquals(-9, f(2));
		// assertEquals(f(2), 8);
	}

	@Test
	void fail() {
		assertEquals(3, f(0));
		// assertEquals(f(1), 7);
	}
}
