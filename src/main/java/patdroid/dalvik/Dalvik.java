package patdroid.dalvik;

import patdroid.core.ClassInfo;
import patdroid.util.Log;

/**
 * Provide common support for dalvik VM
 * <p>
 * The class name convention difference (Java vs. Dalvik):
 * <table summary="">
 * <thead>
 * <tr><th>Case</th><th>Java</th><th>Dalvik</th></tr>
 * </thead>
 * <tbody>
 * <tr><td>String.class.getName()</td><td>"java.lang.String"</td><td>"Ljava/lang/String;"</td></tr>
 * <tr><td>byte.class.getName()</td><td>"byte"</td><td>"B"</td></tr>
 * <tr><td>(new Object[3]).getClass().getName()</td><td>"[Ljava.lang.Object;"</td><td>"[Ljava/lang/Object;"</td></tr>
 * <tr><td>(new int[3][4][5][6][7][8][9]).getClass().getName()</td><td>"[[[[[[[I"</td><td>"[[[[[[[I"</td></tr>
 * </tbody>
 * </table>
 */
public class Dalvik {
    /**
     * Convert an Android class identifier to its canonical form.
     * @param dalvikName the dalvik-style class name
     * @return canonical java class name
     */
    public static String toCanonicalName(String dalvikName) {
        dalvikName = dalvikName.replace('/', '.');
        char first = dalvikName.charAt(0);

        switch (first) {
            case 'C': return "char";
            case 'I': return "int";
            case 'B': return "byte";
            case 'Z': return "boolean";
            case 'F': return "float";
            case 'D': return "double";
            case 'S': return "short";
            case 'J': return "long";
            case 'V': return "void";
            case 'L': return dalvikName.substring(1, dalvikName.length() - 1);
            case '[': return dalvikName;
            default:
                Log.err("unknown dalvik type:" + dalvikName);
                return "";
        }
    }

    /**
     * Convert a canonical Java class name to Dalvik flavor.
     * @param canonicalName canonical java class name
     * @return the dalvik-style class name
     */
    public static String toDalvikName(String canonicalName) {
        final boolean isArray = (canonicalName.charAt(0) == '[');
        if (isArray) {
            return canonicalName.replace('.', '/');
        } else {
            if (canonicalName.equals("char"))
                return "C";
            else if (canonicalName.equals("int"))
                return "I";
            else if (canonicalName.equals("byte"))
                return "B";
            else if (canonicalName.equals("boolean"))
                return "Z";
            else if (canonicalName.equals("float"))
                return "F";
            else if (canonicalName.equals("double"))
                return "D";
            else if (canonicalName.equals("short"))
                return "S";
            else if (canonicalName.equals("long"))
                return "J";
            else if (canonicalName.equals("void"))
                return "V";
            else
                return "L" + canonicalName.replace('.', '/') + ";";
			/* only on Java7
			switch (canonicalName) {
			case "char": return "C";
			case "int": return "I";
			case "byte": return "B";
			case "boolean": return "Z";
			case "float": return "F";
			case "double": return "D";
			case "short": return "S";
			case "long": return "J";
			case "void": return "V";
			default: return "L" + canonicalName.replace('.', '/') + ";";
			}
			*/
        }
    }

    /**
     * Find a ClassInfo by Dalvik class name
     *
     * @param dalvikClassName The class name in Dalvik flavor
     * @return The ClassInfo
     */
    public static ClassInfo findOrCreateClass(String dalvikClassName) {
        return ClassInfo.findOrCreateClass(toCanonicalName(dalvikClassName));
    }

    /**
     * Find a ClassInfo by Dalvik class name
     *
     * @param dalvikClassName The class name in Dalvik flavor
     * @return The ClassInfo
     */
    public static ClassInfo findClass(String dalvikClassName) {
        return ClassInfo.findClass(toCanonicalName(dalvikClassName));
    }

    /**
     * Find a bunch of ClassInfos by Dalvik class name
     *
     * @param dalvikNames The class names in Dalvik flavor
     * @return The ClassInfo
     */
    public static ClassInfo[] findOrCreateClass(String[] dalvikNames) {
        ClassInfo[] ci = new ClassInfo[dalvikNames.length];
        for (int i = 0; i < dalvikNames.length; ++i) {
            ci[i] = findOrCreateClass(dalvikNames[i]);
        }
        return ci;
    }

    /**
     * Find a bunch of ClassInfos by Dalvik class name
     *
     * @param dalvikNames The class names in Dalvik flavor
     * @return The ClassInfo
     */
    public static ClassInfo[] findClass(String[] dalvikNames) {
        ClassInfo[] ci = new ClassInfo[dalvikNames.length];
        for (int i = 0; i < dalvikNames.length; ++i) {
            ci[i] = findClass(dalvikNames[i]);
        }
        return ci;
    }

}
