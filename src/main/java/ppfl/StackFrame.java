package ppfl;


import java.util.Deque;
import java.util.Iterator;
import java.util.ArrayDeque;

import ppfl.instrumentation.DynamicTrace;
import ppfl.instrumentation.TraceDomain;

public class StackFrame {
    private Deque<RuntimeFrame> stackframe;

    public StackFrame(){
        this.stackframe = new ArrayDeque<>();
    }


    // should be called while returning.
	// e.g. return ireturn
	public void pop() {
		this.stackframe.pop();
		// buggy.
		// .runtimestack.clear();
	}

	public RuntimeFrame top() {
		return this.stackframe.peek();
	}

    // should be called while invoking.
	// e.g. invokestatic
	public void push(TraceDomain domain, boolean valid) {
		RuntimeFrame topush = new RuntimeFrame(domain, valid);
		this.stackframe.push(topush);
	}

	public void clear(){
		this.stackframe.clear();
	}

	public void print(MyWriter debugLogger){
		debugLogger.write("\n stackframe:");
		for(RuntimeFrame f : stackframe){
			debugLogger.write("\n	" + f.getName());
			// f.printMap(debugLogger);
			// if(f.getInvoke() != null)
			// 	debugLogger.write(",	invoke = " + f.getInvoke().invokeTrace.toString());
		}
		debugLogger.write("\n");
	}

	public void handleException(Deque<String> stack){
		// System.out.println("\nstack:");
		// for(String s : stack)
		// 	System.out.println(s);
		
		// System.out.println("frame:");
		// for(RuntimeFrame f : stackframe){
		// 	TraceDomain modelFrame =  f.getDomain();
		// 	String modelName = modelFrame.traceclass + "#" + modelFrame.tracemethod;
		// 	System.out.println(modelName);
		// }

		TraceDomain bottomFrame = stackframe.peekLast().getDomain();
		String bottomName = bottomFrame.traceclass + "#" + bottomFrame.tracemethod;
		while(!stack.peek().equals(bottomName)){
			stack.pop();
		}
		assert(stack.peekLast().equals("ppfl.instrumentation.CallBackIndex#logStack"));
		stack.removeLast();

		if(stackframe.size() < stack.size()) // exception message in untraced method trails
			return;

		while(stackframe.size() > stack.size()){
			this.stackframe.pop();
		}

		Iterator<RuntimeFrame> it = stackframe.descendingIterator();
		for(String logName : stack){
			TraceDomain modelFrame =  it.next().getDomain();
			String modelName = modelFrame.traceclass + "#" + modelFrame.tracemethod;
			assert(logName.equals(modelName));
		}
		this.top().clearStack();
	}


}
