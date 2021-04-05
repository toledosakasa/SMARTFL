package ppfl;

import java.io.IOException;
import java.io.Writer;

public class MyWriter {
  public MyWriter(Writer writer) {
    this.writer = writer;
  }

  Writer writer;

  public synchronized void write(String s) {
    try {
      writer.write(s);
      writer.flush();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  public synchronized void writeln(String s) {
    if (!s.endsWith("\n"))
      s = s + "\n";
    try {
      writer.write(s);
      writer.flush();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  public void write(String format, Object... args) {
    write(String.format(format, args));
  }

  public void writeln(String format, Object... args) {
    writeln(String.format(format, args));
  }

  public void flush() {
    try {
      writer.flush();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  public void close() {
    try {
      writer.close();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }
}
