package trace;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

public class PredTest6 {
    public static int foo(int a) {
        if(a<3){ 
            a = 10*a;
        }   
        else{
            a = a-1; //a=a-2;
        }
        return a;
	}

	@Test
	void pass() {
		assertEquals(foo(1), 10);
	}

	@Test
	void fail() {
		assertEquals(foo(4), 2);
	}
}