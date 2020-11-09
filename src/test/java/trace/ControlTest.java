package trace;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

public class ControlTest {
    public static int foo(int a) {
        int rst = 0;
        while(a<10)
        {
            if(a>5){
                if(a>8){ //should be >7
                    return rst+10;
                }
                else{
                    rst++;
                }
            }
            else{
                rst-=100;
            }
            a++;
        }
        return rst;
	}

	@Test
	void pass() {
		assertEquals(foo(9), 10);
	}

	@Test
	void fail() {
		assertEquals(foo(0), -588);
	}
}
