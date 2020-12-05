package trace;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

public class PredTest3 {
    public static int foo(int a) {
        int b = a *a;
        if(a<3){ //a<2
            b = b +a;
        }
        return b;
	}

	@Test
	void pass() {
		assertEquals(foo(1), 2);
	}

	@Test
	void fail() {
		assertEquals(foo(2), 4);
	}
}