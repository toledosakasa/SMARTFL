package trace;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

public class PredTest7 {
    public static int foo(int a, int b) {
        int c = 0;
        int d = 0;
        int e = 0;
        int f = 0;
        if(a<3){ //b<3
            a = (a +b)*(a-b)+2*a*b;
            if(a == 2){
                d = 1;
            }
            else{
                e = 2;
            }
        }
        else{
            a = a;
            f = 1;
        }
        b = a*b;
        return b+e+f+d;
	}

	@Test
	void pass() {
		assertEquals(foo(1,1), 3);
	}

	// @Test
	// void fail() {
	// 	assertEquals(foo(5,1), 34);
	// }
}