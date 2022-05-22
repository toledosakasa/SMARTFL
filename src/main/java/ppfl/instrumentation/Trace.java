package ppfl.instrumentation;

public class Trace {
    private int opcode;
    String classmethodname;

    Trace(int opcode, String name){
        this.opcode = opcode;
        this.classmethodname = name;
    }
}
