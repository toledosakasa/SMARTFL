package trace;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class DomainTest {

	//private static java.util.logging.Logger ppfl_logger = java.util.logging.Logger.getLogger("trace.DomainTest");
	static boolean b[] = {true, true,false};
	
	public static int f(int a) throws Exception {
		boolean c = b[2];
		if (a > 1)
			a = a + 1;
		a = a + 2;
//		while(a < 7) {
//			a++;
//			if(a == 6) {
//				break;
//			}
//		}
			
//		if(a != 10) {
//			throw new Exception();
//		}
//        double x = 1.0;
//        double y = 1.1;
//        double z = 1.0;
//        int zz = 1;
//        int ttt = 2;
//        if(x>y)
//            a= a+1;
		return a;
	}

	@Test
	void test() {
		try {
			f(2);
		}
		catch(Exception e) {
			
		}
		
	}

}
