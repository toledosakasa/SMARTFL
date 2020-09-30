import com.sun.btrace.annotations.*;
import static com.sun.btrace.BTraceUtils.*;

@BTrace
public class BranchTest_pattern {
    @OnMethod(
        clazz="trace.BranchTest",
        method="/.*/",
        location=@Location(value=Kind.LINE, line=-1)
    )
    public static void online(@ProbeClassName String pcn, @ProbeMethodName String pmn, int line) {
        print(pcn + "." + pmn +  ":" + line + "\n");
    }
    
}
