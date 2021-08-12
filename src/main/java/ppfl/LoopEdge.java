package ppfl;


public class LoopEdge implements Comparable<LoopEdge>{
    public String start;
    public String end;
    public int length;

    public LoopEdge(String start, String end) {
        this.start = start;
        this.end = end;
        this.length = Integer.valueOf(start.split("#")[4]) - Integer.valueOf(end.split("#")[4]);
        if (this.length < 0)
            this.length = -this.length;
    }

    @Override
    public int compareTo(LoopEdge o) {
        if (this.length == o.length)
            return this.start.compareTo(o.start);
        else
            return Integer.compare(this.length,o.length);
    }
}
