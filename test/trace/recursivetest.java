package trace;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class recursivetest {

	public static int fact(int n) {
		if (n == 0)
			return 1;
		else {//it is suggested to separate function call and return.
			int ret = fact(n - 1);// untested bug may occur when nested call\other complicated situation.
			return n * ret;//maybe SSA-like source is needed. program transformation?
		}
	}

	@Test
	void test() {
		fact(5);
	}

}
