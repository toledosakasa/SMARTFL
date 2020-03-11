package trace;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class recursivetest {
	
	public static int fact(int n)
	{            
        if (n==0)
            return 1;
        else
            return n*fact(n-1);
	}  

	@Test
	void test() {
		fact(5);
	}

}
