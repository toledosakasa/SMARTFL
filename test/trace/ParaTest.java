package trace;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class ParaTest {

	public static double func2(double num, double den) {
		double res = num/(den+1);
		return res;
	}
	
	public static double func(double a) {
		double b=a*a;
		double res=func2(b,a);//func2(a,b)
		return res;
	}
	
	@Test
	void pass() {
		assertEquals(func(1), 0.5);
	}
	
	@Test
	void fail() {
		assertEquals(func(2),0.4);
	}
}
