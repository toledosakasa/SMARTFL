package trace;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class MultiTest {
    // public int A;
    // public int B;
    // public int C;
    // public int D;
    // public int E;
	public int foo(int n) {
        int cnt = 1;
        // int res = 0;
        int a0=n-2;
        int a1=0;
        int a2=0;
        int a3=0;
        int a4=0;
        int a5=0;
        int a6=0;
        int a7=0;
        int a8=0;
        int a9=0;
        int a10=0;
        int a11=0;
        int a12=0;
        int a13=0;
        int a14=0;
        int a15=0;
        int a16=0;
        int a17=0;
        int a18=0;
        int a19=0;
        int a20=0;
        int a21=0;
        int a22=0;
        int a23=0;
        int a24=0;
        int a25=0;
        int a26=0;
        int a27=0;
        int a28=0;
        int a29=0;
        for(;cnt<n;cnt++){ //<=n
            // a0++;
            a1+=a0;
            a2+=a1;
            a3+=a2;
            a4+=a3;
            a5+=a4;
            a6+=a5;
            a7+=a6;
            a8+=a7;
            a9+=a8;
            a10+=a9;
            a11+=a10;
            a12+=a11;
            a13+=a12;
            a14+=a13;
            a15+=a14;
            a16+=a15;
            a17+=a16;
            a18+=a17;
            a19+=a18;
            a20+=a19;
            a21+=a20;
            a22+=a21;
            a23+=a22;
            a24+=a23;
            a25+=a24;
            a26+=a25;
            a27+=a26;
            a28+=a27;
            if(cnt == 5){
                a29+=10; //error
            }
            a29+=a28;
        }
        return a29;
	}

	@Test
	void pass() {
		assertEquals(foo(2), 0);
    }
    
    @Test
	void fail() {
		assertEquals(foo(10), 0);
	}

}

