package trace;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

public class PredTest {
	public static int foo(int a) {
		if (a < 3) { // should be a<2
			a++;
		}
		return a;
	}

	@Test
	void pass() {
		assertEquals(2, foo(1));
	}

	@Test
	void fail() {
		assertEquals(2, foo(2));
	}
}
