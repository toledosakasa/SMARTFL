package trace;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;


public class TrycatchTest {
	static void procedure(int a) {
        try {
            int b =42/a;
        } catch(java.lang.ArithmeticException e) {
            System.out.println("inprocedure, catch ArithmeticException: " + e);
        }
    }
    /*public static void main(String args[]) {
        try {
            procedure();
        } catch(java.lang. Exception e){
            System.out.println("inmain, catch Exception: " + e);
        }
    }*/
	
    @Test
	void pass() {
    	/*try {
            procedure(0);
        } catch(java.lang. Exception e){
            System.out.println("catch Exception: " + e);
        }*/
    	procedure(0);
	}
}
