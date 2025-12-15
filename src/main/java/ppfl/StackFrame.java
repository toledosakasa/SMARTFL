package ppfl;


import java.util.Deque;
import java.util.Iterator;
import java.util.ArrayDeque;
import java.util.Set;
import java.util.HashSet;

import ppfl.RuntimeFrame.FrameState;
import ppfl.instrumentation.DynamicTrace;
import ppfl.instrumentation.TraceDomain;

public class StackFrame {
    public Deque<RuntimeFrame> stackframe;

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
	public void push(TraceDomain domain, FrameState state) {
		RuntimeFrame topush = new RuntimeFrame(domain, state);
		this.stackframe.push(topush);
	}

	public void clear(){
		this.stackframe.clear();
	}

	public void print(MyWriter debugLogger){
		debugLogger.write("\n stackframe:");
		for(RuntimeFrame f : stackframe){
			debugLogger.write("\n	" + f.getName() + ", state = " + f.getState());
			// f.printMap(debugLogger);
			// if(f.getInvoke() != null)
			// 	debugLogger.write(",	invoke = " + f.getInvoke().invokeTrace.toString());
		}
		debugLogger.write("\n");
	}

	private int countSize(){
		int ret = 0;
		for(RuntimeFrame f : stackframe){
			if(f.getState() != FrameState.Untraced)
				ret += 1;
		}
		return ret;
	}

	public void handleException(Deque<String> stack, MyWriter debugLogger){
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

		Set<String> frameSet = new HashSet<>();
		for(RuntimeFrame frame : stackframe){
			TraceDomain domain = frame.getDomain();
			if(frame.getState() == FrameState.Untraced)
				continue;
			String frameName = domain.traceclass + "#" + domain.tracemethod;
			frameSet.add(frameName);
		}

		// debugLogger.write("\n frame set = " + frameSet);

		Set<String> removeSet = new HashSet<>();
		for(String stackframe : stack){
			if(!frameSet.contains(stackframe))
				removeSet.add(stackframe);
		}

		// debugLogger.write("\n remove set = " + removeSet);
		
		stack.removeAll(removeSet);

		// debugLogger.write("\n stack = " + stack);

		if(stackframe.size() < stack.size()){ // exception message in untraced method trails
			this.top().clearStack();
			return;
		}

		while(countSize() > stack.size()){
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
