package patdroid.regtest;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Ordering;
import com.google.common.io.Files;
import com.google.common.io.PatternFilenameFilter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import patdroid.core.ClassInfo;
import patdroid.core.MethodInfo;
import patdroid.dalvik.Instruction;
import patdroid.smali.SmaliClassDetailLoader;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;
import java.util.zip.ZipFile;

import static org.junit.Assert.assertEquals;

/**
 * Regression test: reads APK and compare with dump files.
 *
 * <p>Specify -Dregtest.apkpath=<apk path> to run regression test.
 * <p>Also specify -Dregtest.updatedump=true to update dump files.
 */
@RunWith(Parameterized.class)
public class RegTest {
    private static final int API_LEVEL = 19;
    private static final Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    private final File apkFile;
    private final File dumpFile;
    private final boolean updateDump;
    private BufferedReader dumpReader;
    private BufferedWriter dumpWriter;
    private int lineNumber;

    public RegTest(File apkFile, File dumpFile, boolean updateDump) {
        this.apkFile = apkFile;
        this.dumpFile = dumpFile;
        this.updateDump = updateDump;
    }

    @Before
    public void setUp() throws IOException {
        if (updateDump) {
            this.dumpWriter = new BufferedWriter(new FileWriter(dumpFile));
        } else {
            this.dumpReader = new BufferedReader(new FileReader(dumpFile));
        }
        this.lineNumber = 0;
    }

    @After
    public void tearDown() throws IOException {
        if (updateDump) {
            this.dumpWriter.close();
        }
    }

    @Test
    public void run() throws IOException {
        SmaliClassDetailLoader.getFrameworkClassLoader(API_LEVEL).loadAll();
        new SmaliClassDetailLoader(new ZipFile(apkFile), true).loadAll();

        List<ClassInfo> sortedClasses = Ordering.usingToString().sortedCopy(ClassInfo.getAllClasses());
        for (ClassInfo c : sortedClasses) {
            if (!c.isFrameworkClass()) {
                handleLine(c.toString());
                List<MethodInfo> sortedMethods = Ordering.usingToString().sortedCopy(c.getAllMethods());
                for (MethodInfo m : sortedMethods) {
                    handleLine("\t" + m);
                    if (m.insns == null) {
                        handleLine("\t\t(no instructions)");
                    } else {
                        for (Instruction i : m.insns) {
                            handleLine("\t\t" + i);
                        }
                    }
                }
            }
        }
    }

    private void handleLine(String line) throws IOException {
        ++lineNumber;
        if (updateDump) {
            this.dumpWriter.write(line);
            this.dumpWriter.newLine();
        } else {
            assertEquals("line " + lineNumber, this.dumpReader.readLine(), line);
        }
    }

    @Parameterized.Parameters
    public static List<Object[]> params() {
        String apkPath = System.getProperty("regtest.apkpath", "");
        if (apkPath.isEmpty()) {
            logger.warning("regtest.apkpath not set, skipping regression test.");
            return ImmutableList.of();
        }
        String updateDumpProperty = System.getProperty("regtest.updatedump", "");
        boolean updateDump = updateDumpProperty.equalsIgnoreCase("true");
        if (updateDump) {
            logger.info("Updating dump for apks in " + apkPath);
        } else {
            logger.info("Running regression test for apks in " + apkPath);
        }
        File apkDir = new File(apkPath);
        File[] apkFiles = apkDir.listFiles(new PatternFilenameFilter("^.*.apk"));
        ImmutableList.Builder<Object[]> params = ImmutableList.builder();
        for (File apkFile : apkFiles) {
            String dumpFileName = Files.getNameWithoutExtension(apkFile.getName()) + ".txt";
            File dumpFile = new File(apkFile.getParentFile(), dumpFileName);
            params.add(new Object[]{apkFile, dumpFile, updateDump});
        }
        return params.build();
    }
}
