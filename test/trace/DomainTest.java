package trace;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class DomainTest {

	public static int f(int a) {
		if (a > 1)
			a = a + 1;
		a = a + 2;
		return a;
	}

	@Test
	void test() {
		f(2);
	}

}
