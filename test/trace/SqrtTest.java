package trace;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class SqrtTest {

	final double mEpsilon = 1e-7;
	public double sqrt(double N){
		double x = N;
		double M = N;
		double m = 1;
		double r = x;
		double diff = x * x - N;
		while(Math.abs(diff) > mEpsilon){
			if(diff < 0){
				m = x;
				x = (M + x) / 2;

			}
			else if(diff > 0){
				M = x;
				x = (m + x) / 2;

			}
			diff = x * x - N;

		}
		r = x;
		return r;
	}

	
	@Test
	void test() {
		sqrt(1234567.0);
	}

}
