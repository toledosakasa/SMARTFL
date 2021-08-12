package ppfl;

import java.io.IOException;
import java.io.Writer;
import java.util.HashSet;
import java.util.Set;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;
import javassist.bytecode.BadBytecode;
import javassist.bytecode.CodeIterator;
import javassist.bytecode.ConstPool;

public class ProfileUtils {
  static Set<String> d4jMethodNames;
  static Set<String> profileCnt;
  public static int logindex = 0;
  public static Writer writer = null;
  static boolean started = false;

  public static void init(ConstPool constp, Writer traceWriter) {
    if (true) {
      started = true;
      ClassPool cp = ClassPool.getDefault();
      CtClass thisKlass = null;
      try {
        thisKlass = cp.get("ppfl.ProfileUtils");
      } catch (NotFoundException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      int classindex = constp.addClassInfo(thisKlass);
      logindex = constp.addMethodrefInfo(classindex, "logMethodName_inst", "(Ljava/lang/String;)V");
    }
    writer = traceWriter;
  }

  public static void setD4jMethods(Set<String> s) {
    d4jMethodNames = s;
    // if (!started)
    // System.out.println(s);
  }

  public static boolean isD4jTestMethod(String longname) {
    return d4jMethodNames.contains(longname);
  }

  public static void logMethodName(CodeIterator ci, String longname, ConstPool constp) {
    int instpos = 0;
    try {
      instpos = ci.insertGap(6);
    } catch (BadBytecode e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    int instindex = constp.addStringInfo(longname);
    ci.writeByte(19, instpos);// ldc_w
    ci.write16bit(instindex, instpos + 1);

    ci.writeByte(184, instpos + 3);// invokestatic
    ci.write16bit(logindex, instpos + 4);
  }

  public static synchronized void logMethodName_inst(String str) {
    // if (str.contains("setUp")) {
    // StackTraceElement stack[] = Thread.currentThread().getStackTrace();
    // for (int i = 0; i < stack.length; i++) {
    // System.out.print("\t" + stack[i].getClassName() + "#" +
    // stack[i].getMethodName());
    // }
    // }

    // FIXME deal with junit setUp method.
    if (isD4jTestMethod(str)) {
      profileCnt = new HashSet<>();
      // System.out.println(str);
    }
    if (profileCnt != null && profileCnt.contains(str)) {
      return;
    }
    if (profileCnt != null)
      profileCnt.add(str);
    try {
      writer.write("\n###" + str);
      writer.flush();
    } catch (IOException e) {  
      e.printStackTrace();
    }
  }
}
