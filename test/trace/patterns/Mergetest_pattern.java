import com.sun.btrace.annotations.*;
import static com.sun.btrace.BTraceUtils.*;

@BTrace
public class Mergetest_pattern {
    @OnMethod(
        clazz="trace.Mergetest",
        method="/.*/",
        location=@Location(value=Kind.LINE, line=-1)
    )
    public static void online(@ProbeClassName String pcn, @ProbeMethodName String pmn, int line) {
        print(pcn + "." + pmn +  ":" + line + "\n");
    }
    
}
