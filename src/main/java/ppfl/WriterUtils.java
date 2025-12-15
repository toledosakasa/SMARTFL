package ppfl;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class WriterUtils {
  private static WriterUtils instance = new WriterUtils();
  public static Map<String, MyWriter> wMap = new HashMap<>();
  static final int BUFFERSIZE = 1024;
  static String path = null;
  public static boolean running = true;

  public static Thread cleanup() {
    return new Thread() {
      @Override
      public void run() {
        if (!running)
          return;
        running = false;
        for (Entry<String, MyWriter> entry : wMap.entrySet()) {
          entry.getValue().flush();
          entry.getValue().close();
        }
      }
    };
  }

  private WriterUtils() {
    Runtime.getRuntime().addShutdownHook(cleanup());
  }

  public static void setPath(String newPath) {
    path = newPath;
    File file = new File(path);
    if (!file.exists()) {
      file.mkdirs();
    }
  }

  // public static WriterUtils getInstance() {
  // return instance;
  // }

  public static MyWriter getWriter(String parent, String child) {
    setPath(parent);
    return getWriter(child, false);
  }

  public static MyWriter getWriter(String name) {
    return getWriter(name, false);
  }

  public static MyWriter getWriter(String name, boolean append) {
    if (!name.endsWith(".log"))
      name = name + ".log";
    if (!wMap.containsKey(name)) {
      File file = new File(path, name);
      BufferedWriter bWriter = null;
      try {
        if (!file.exists()) {
          file.createNewFile();
        }
        bWriter = new BufferedWriter(new FileWriter(file, append), BUFFERSIZE);
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      wMap.put(name, new MyWriter(bWriter));
    }
    return wMap.get(name);
  }

  public static String handleException(Exception e){
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    e.printStackTrace(pw);
    String sStackTrace = sw.toString(); // stack trace as a string
    return sStackTrace;
  }

}
