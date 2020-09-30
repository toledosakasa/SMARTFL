package trace;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class sumtest {
	static int[] array = new int[10];

	public static int sum() {
		for (int i = 0; i < 9; i++)
			array[i] = i;
		array[9] = 10;
		int r = 0;
		for (int i = 0; i < 9; i++)
			r = r + i;
		return r;
	}

	@Test
	void test() {
		sum();
	}

}
