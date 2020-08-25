package trace;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class FullTest {
	public static double full(double x, double y) {
		double tmp = 0;
		if (x < 0)//should be (x<-1)
		{
			x=x+1;
			y=y+1; 
			if(y>0)
				tmp = x/y;
			else
				tmp = y/x;
		}
		else if (x > 0)//should be (x>1) 
		{
			x = x-1;
			y = y-1; 
			if(y > 0)
				tmp = x/y;
			else
				tmp = y/x;
		}
		else
			return 0;
		return tmp;
	}
	
	@Test
	void pass() {
		assertEquals(full(0,0), 0);//29
	}
	
	@Test
	void pass1() {
		assertEquals(full(-2,1), -0.5);//24
	}
	
	@Test
	void pass2() {
		assertEquals(full(-2,-3),2);//26
	}
	
	@Test
	void pass3() {
		assertEquals(full(2,3),0.5);//15
	}
	
	@Test
	void pass4() {
		assertEquals(full(2,-1),-2);//17
	}
	
	@Test
	void fail1() {
		assertEquals(full(-0.5,1), 0);//24
	}
	
	@Test
	void fail2() {
		assertEquals(full(-0.5,-3),0);//26
	}
	
	@Test
	void fail3() {
		assertEquals(full(0.5,3),0);//15
	}
	
	@Test
	void fail4() {
		assertEquals(full(0.5,-1),0);//17
	}
	
}
