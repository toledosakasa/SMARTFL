package trace;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

public class PredTest5 {
    public static int foo(int a, int b) {
        if(a<3){ 
            a = 10*a;
        }
        if(b < 3){ // b<2
            b = b*10;
        }
        return a+b;
	}

	@Test
	void pass() {
		assertEquals(foo(1,1), 20);
	}

	@Test
	void fail() {
		assertEquals(foo(2,2), 22);
	}
}