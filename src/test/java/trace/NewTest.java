package trace;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class NewTest {
	
	public static int f1(int a) {
		if (a > 1)
			a = a + 1;
		a = a + 2;
		return a;
	}
	
	@Test
	void pass() {
		NewTest C1 = new NewTest();
		assertEquals(C1.f1(2), 5);
	}
	
	@Test
	void fail() {
		NewTest C2 = new NewTest();
		assertEquals(C2.f1(1), 4);
	}
}
