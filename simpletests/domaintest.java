public class foo {

	public static int f(int a)
	{
		if(a>1)
			a = a + 1;
		a = a + 2;
		return a;
	}  
	public static void main(String []args) {
       int b = 3;
       int c = b + 1;
       int a = 12;
       f(2);
    }
} 