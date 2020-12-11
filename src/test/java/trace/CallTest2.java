package trace;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

public class CallTest2 {

    public static int foo(String a ) {
        if(a.equals("test")) //"test1"
            return 1;
        else
            return 0;
	}

	@Test
	void pass() {
		assertEquals(foo("bar"), 0);
	}

	@Test
	void fail() {
		assertEquals(foo("test1"), 1);
	}
}