package trace;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class DomainTest {

	//private static java.util.logging.Logger ppfl_logger = java.util.logging.Logger.getLogger("trace.DomainTest");
	
	public static int f(int a) {
		short b = 1;
		System.out.println("sdf");
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
