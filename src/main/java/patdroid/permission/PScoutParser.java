package patdroid.permission;

import patdroid.core.ClassInfo;
import patdroid.core.MethodInfo;

import java.io.*;

/**
 * A parser for the output file of PScout from UToronto.
 * See http://pscout.csl.toronto.edu/ for more details
 */
public class PScoutParser {
   public static APIMapping parse(File f) throws IOException {
      final APIMapping r = new APIMapping();
      final BufferedReader br = new BufferedReader(new FileReader(f));
      String perm = "", line = br.readLine();
      while (line != null) {
         perm = line.replace("Permission:", "");
         br.readLine(); // skip a line telling how many callers in total
         do {
            line = br.readLine();
            if (line == null || !line.startsWith("<")) {
               break;
            }
            MethodInfo m = parseMethod(line);
            r.add(m, perm);
         } while (true);
      }
      br.close();
      return r;
   }

   private static MethodInfo parseMethod(String line) {
      // example: <android.net.wifi.WifiManager: boolean reassociate()>
      String className, returnType, methodName;
      String[] paramTypes;
      String s = line.substring(1, line.length() - 1);
      String[] a = s.split(":"); // class, rest
      className = a[0];
      s = a[1];
      int pos = s.indexOf('(');
      a[0] = s.substring(0, pos); // ret methodName
      a[1] = s.substring(pos + 1, s.length() - 1); // params
      returnType = a[0].trim().split(" ")[0];
      methodName = a[0].trim().split(" ")[1];
      paramTypes = a[1].replace(" ", "").split(",");
      final MethodInfo mproto = MethodInfo.makePrototype(methodName,
              findOrCreateClass(returnType),
              findOrCreateClass(paramTypes),
              0);
      ClassInfo ci = ClassInfo.findOrCreateClass(className);
      return (ci == null ? null : ci.findMethod(mproto));
   }

   /**
    * Convert PSCout-style type name to canonical form
    * @param t
    * @return
    */
   private static ClassInfo findOrCreateClass(String t) {
      if (!t.endsWith("[]")) {
         return ClassInfo.findOrCreateClass(t);
      } else {
         String baseType = t.substring(0, t.indexOf("[]"));
         int level = (t.length() - t.indexOf("[]")) / 2;
         String s = "";
         for (int i = 0; i < level; ++i)
            s += "[";
         // TODO: map all primitive types to short form
         if (baseType.equals("int"))
            s += "I";
         else if (baseType.equals("boolean"))
            s += "B";
         else
            s += "L" + baseType + ";";
         return ClassInfo.findOrCreateClass(s);
      }
   }

   public static ClassInfo[] findOrCreateClass(String[] fullNames) {
      final ClassInfo[] a = new ClassInfo[fullNames.length];
      for (int i = 0; i < fullNames.length; ++i) {
         a[i] = findOrCreateClass(fullNames[i]);
      }
      return a;
   }

}
