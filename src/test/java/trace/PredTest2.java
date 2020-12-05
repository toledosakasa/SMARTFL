package trace;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

public class PredTest2 {
    public static int foo(int a) {
        a = a +4; // a+2
        int b = 0;
        if(a<5){ 
            b = 10;
        }
        else{
            b = -10;
        }
        return b;
	}

	@Test
	void pass() {
		assertEquals(foo(4), -10);
	}

	@Test
	void fail() {
		assertEquals(foo(2), 10);
	}
}