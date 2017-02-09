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
import static org.junit.Assert.assertNotNull;

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
        ClassInfo.globalScope.reset();
    }

    @Test
    public void run() throws IOException {
        if (updateDump) {
            logger.info("Updating dump for " + apkFile);
        } else {
            logger.info("Running regression test for " + apkFile);
        }

        SmaliClassDetailLoader.getFrameworkClassLoader(API_LEVEL).loadAll();
        new SmaliClassDetailLoader(new ZipFile(apkFile), true).loadAll();

        List<ClassInfo> sortedClasses = Ordering.usingToString().sortedCopy(ClassInfo.getAllClasses());
        for (ClassInfo c : sortedClasses) {
            if (c.isFrameworkClass()) {
                continue;
            }
            handleEntry(c.toString());
            if (c.isMissing()) {
                handleEntry("\t(missing class)");
                continue;
            }
            List<MethodInfo> sortedMethods = Ordering.usingToString().sortedCopy(c.getAllMethods());
            for (MethodInfo m : sortedMethods) {
                handleEntry("\t" + m);
                if (m.insns == null) {
                    handleEntry("\t\t(no instructions)");
                } else {
                    for (Instruction i : m.insns) {
                        handleEntry("\t\t" + i);
                    }
                }
            }
        }
    }

    private void handleEntry(String entry) throws IOException {
        BufferedReader entryReader = new BufferedReader(new StringReader(entry));
        String line;
        while ((line = entryReader.readLine()) != null) {
            ++lineNumber;
            if (updateDump) {
                this.dumpWriter.write(line);
                this.dumpWriter.newLine();
            } else {
                assertEquals("line " + lineNumber, this.dumpReader.readLine(), line);
            }
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
        File apkDir = new File(apkPath);
        File[] apkFiles = apkDir.listFiles(new PatternFilenameFilter("^.*.apk"));
        assertNotNull("failed to list files", apkFiles);
        ImmutableList.Builder<Object[]> params = ImmutableList.builder();
        for (File apkFile : apkFiles) {
            String dumpFileName = Files.getNameWithoutExtension(apkFile.getName()) + ".txt";
            File dumpFile = new File(apkFile.getParentFile(), dumpFileName);
            params.add(new Object[]{apkFile, dumpFile, updateDump});
        }
        return params.build();
    }
}
