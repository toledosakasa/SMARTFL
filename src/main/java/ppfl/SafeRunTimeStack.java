package ppfl;

import java.util.ArrayDeque;
import java.util.Deque;

public class SafeRunTimeStack {
  Deque<Node> stack;
  boolean compromise = false;

  public SafeRunTimeStack() {
    stack = new ArrayDeque<>();
  }

  public boolean isEmpty() {
    return stack.isEmpty();
  }

  public Node peek() {
    if (stack.isEmpty()) {
      if (compromise)
        return null;
      else {
        return stack.peek();// will crash
      }
    } else {
      return stack.peek();
    }
  }

  public Node pop() {
    if (stack.isEmpty()) {
      if (compromise)
        return null;
      else {
        return stack.pop();// will crash
      }
    } else {
      return stack.pop();
    }
  }

  public int size() {
    return stack.size();
  }

  public void push(Node n) {
    stack.push(n);
  }
}
