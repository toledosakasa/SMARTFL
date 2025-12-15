package ppfl;

import java.util.List;
import ppfl.instrumentation.DynamicTrace;

public class InvokeItem{
    public StmtNode stmt;
    public List<Node> use;
    public List<Node> pred;
    public DynamicTrace invokeTrace = null;
    public int argcnt = 0;
    public Node callerObj = null;
    public boolean canBeParsed;

    public enum InvokeState{
        Start, Traced, Untraced
    }

    public InvokeState state;

    public InvokeItem(StmtNode stmt, List<Node> use, List<Node> pred, 
        DynamicTrace invokeTrace, int argcnt, Node callerObj){
        this.stmt = stmt;
        this.use = use;
        this.pred = pred;
        this.invokeTrace = invokeTrace;
        this.argcnt = argcnt;
        this.callerObj = callerObj;
        this.canBeParsed = true;
        this.state = InvokeState.Start;
    }

}
