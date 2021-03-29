package ppfl;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class WriterUtils {
  private static WriterUtils instance = new WriterUtils();
  public static Map<String, MyWriter> wMap = new HashMap<>();
  static final int BUFFERSIZE = 1048576;
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

  public static MyWriter getWriter(String name) {
    if (!name.endsWith(".log"))
      name = name + ".log";
    if (!wMap.containsKey(name)) {
      File file = new File(path, name);
      BufferedWriter bWriter = null;
      try {
        if (!file.exists()) {
          file.createNewFile();
        }
        bWriter = new BufferedWriter(new FileWriter(file, true), BUFFERSIZE);
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      wMap.put(name, new MyWriter(bWriter));
    }
    return wMap.get(name);
  }
}
