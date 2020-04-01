package trace;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class FourTest {
	public static double devi(double x, double y) {
		double tmp = 0;
		if (x > 0)//should be (x>1) 
		{
			x = x-1;
			y = y-1; //if delete this, an error while parse£¬repaired£¿
			if(y > 0)
				tmp = x/y;
			else
				tmp = y/x;
		}
		else if (x < 0)//should be (x<-1)
		{
			x=x+1;
			y=y+1; //if delete this, an error while parse£¬repaired£¿
			if(y>0)
				tmp = x/y;
			else
				tmp = y/x;
		}
		else
			return 0;
		return tmp;
	}
	
	@Test
	void pass1() {
		assertEquals(devi(0,0), 0);
	}
	
	@Test
	void pass2() {
		assertEquals(devi(1.5,2),0.5);
	}
	
	@Test
	void fail() {
		assertEquals(devi(0.5,0.5),0);
	}
	
	@Test
	void fail2() {
		assertEquals(devi(-0.5,-0.5),0);
	}
	
}
