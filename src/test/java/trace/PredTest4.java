package trace;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

public class PredTest4 {
    public static int foo(int a, int b) {
        if(a<3){ //b<3
            a = (a +b)*(a-b)+2*a*b;
        }
        else{
            a = a;
        }
        b = a*b;
        return b;
	}

	@Test
	void pass() {
		assertEquals(foo(1,1), 2);
	}

	@Test
	void fail() {
		assertEquals(foo(5,1), 34);
	}
}