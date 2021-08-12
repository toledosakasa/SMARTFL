package ppfl;

import java.util.List;

// import org.slf4j.Logger;

public class NWrongFactorNode extends FactorNode {
    private int nwrong;
    private List<Edge> stmtedges;

    public NWrongFactorNode(List<Edge> stmtedges, int nwrong) {
        this.nwrong = nwrong;
        this.stmtedges = stmtedges;
    }

    @Override
    public void send_message() {

        // double othert = 1;
        double othersum = 0;
        for (Edge e : stmtedges) {
            double otherv = e.get_ntof();
            // System.out.println("the otherv = "+otherv + "\n");
            othersum += (1 - otherv) / otherv;
            // othert *= e.get_ntof();
        }
        for (Edge e : stmtedges) {
            // double tmp = othert/e.get_ntof();
            // e.set_fton((1-tmp)/(2-tmp));
            // System.out.println("the tmp = "+tmp + "\n");
            double thisv = e.get_ntof();
            double tmp = othersum - (1 - thisv) / thisv;
            e.set_fton(tmp / (1 + tmp));
            // System.out.println("the tmp = "+tmp + "\n");
        }
    }

    @Override
    public void print(MyWriter lgr) {
        lgr.writeln("has nwrong factor\n");
    }

    @Override
    public void print() {
        debugLogger.writeln("has nwrong factor\n");
    }
}