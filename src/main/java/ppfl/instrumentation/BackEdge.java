package ppfl.instrumentation;

public class BackEdge {
    final public Integer start;
    final public Integer end;

    public BackEdge(Integer start, Integer end) {
        this.start = start;
        this.end = end;
    }
}
