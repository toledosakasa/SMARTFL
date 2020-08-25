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
	
	public static int f2(int b) {
		if (b > 4)
			b = b + 1;
		b = b + 2;
		return b;
	}
	
	public int f(int c) {
		/*a = f1(a);
		a = f2(a);
		return a;*/
		c = f1(c);
		c = f2(c);
		return c;
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
