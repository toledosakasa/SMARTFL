package trace;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

public class ForTest {
    public static int foo(int a) {
        int r = 0;
        for(int i=0;
            i<a; //i<=a
            i++){
            r += i*i;
        }
        return r;
	}

	@Test
	void pass() {
		assertEquals(foo(0), 0);
	}

	@Test
	void fail() {
		assertEquals(foo(1), 1);
	}
}