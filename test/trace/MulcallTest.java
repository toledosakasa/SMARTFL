package trace;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class MulcallTest {
	
	public int f1(int a) {
		if (a > 1)
			a = a + 1;
		a = a + 2;
		return a;
	}
	
	public static int f2(int a) {
		if (a > 4)
			a = a + 1;
		a = a + 2;
		return a;
	}
	
	public int f(int a) {
		a = f1(a);
		a = f2(a);
		return a;
	}
	
	@Test
	void pass() {
		MulcallTest C1 = new MulcallTest();
		assertEquals(C1.f(2), 8);
		//assertEquals(f(2), 8);
	}
	
	@Test
	void fail() {
		MulcallTest C2 = new MulcallTest();
		assertEquals(C2.f(1), 7);
		//assertEquals(f(1), 7);
	}
	
}
