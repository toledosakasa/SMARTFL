package trace;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class NewTest {

	public int f1(int a) {
		if (a > 1)
			a = a + 1;
		a = a + 2;
		return a;
	}

	@Test
	void pass() {
		NewTest C1 = new NewTest();
		assertEquals(5, C1.f1(2));
	}

	@Test
	void fail() {
		NewTest C2 = new NewTest();
		assertEquals(4, C2.f1(1));
	}
}
