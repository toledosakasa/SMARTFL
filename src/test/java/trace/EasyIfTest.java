package trace;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class EasyIfTest {
	public static int addorminus(int x) {
		if (x > 0)// should be (x>2)
		{
			x = x + 1;
        } 
        else
        {
            x = x - 1;
        }
		return x;
    }

    @Test
	void pass1() {
		assertEquals(-1,addorminus(0));
    }
    
    @Test
	void pass2() {
		assertEquals(4,addorminus(3));
    }

    @Test
	void fail1() {
		assertEquals(0,addorminus(1));
    }
    
}