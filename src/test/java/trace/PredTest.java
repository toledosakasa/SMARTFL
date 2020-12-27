package trace;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

public class PredTest {
	public static int foo(int a) {
		if (a < 3) { // a<2
			a++;
		}
		return a;
	}

	@Test
	void pass() {
		assertEquals(foo(1), 2);
	}

	@Test
	void fail() {
		assertEquals(foo(2), 2);
	}
}
