
package com.atelieryl.wonderdroid.utils;

import java.io.File;
import java.io.FileReader;

public class CpuUtils {

    public enum Arch { // Archs,.. I don't think there are any ARMv4 cores that
                       // can run Android ;)
        UNKNOWN, ARMv5, ARMv6, ARMv7
    }

    private static boolean detectionDone = false;

    private static Arch arch = Arch.UNKNOWN;

    private static boolean haveNeon = false;

    private static boolean haveVFP = false;

    private static void doCPUDetection() {

        StringBuilder sb = new StringBuilder();

        try {
            File cpuinfo = new File("/proc/cpuinfo");
            FileReader fr;
            fr = new FileReader(cpuinfo);
            int ch;
            while ((ch = fr.read()) != -1) {
                sb.append((char)ch);
            }
            fr.close();
        } catch (Exception ex) {
            System.out.println("Could't read /proc/cpuinfo.. getting outta here");
            return;
        }

        String[] fields = sb.toString().split("\n");

        boolean processorSeen = false;
        for (String field : fields) {

            // Handle multiple cores
            if (field.startsWith("processor") || field.startsWith("Processor")) { // ARM
                                                                                  // cpuinfo
                                                                                  // has
                                                                                  // fields
                                                                                  // with
                                                                                  // caps
                if (processorSeen) {
                    // found another core or sibling.. get outta here
                    break;
                }
                processorSeen = true;
            }

            // This should parse any ARM Processor field, the issue is that this
            // varies between
            // ports and there is a shit ton of ARM ports.. the line should
            // contain at least ARM (usually the model number ARM???)
            // and the ARM arch version and endian in the format v?<endian> i.e.
            // v7l .. l for little.
            if (field.startsWith("Processor")) { // Probably an ARM
                if (!field.contains("ARM")) { // Not arm?!
                    System.out.println("Coudln't parse precessor field; " + field);
                    break;
                } else {
                    if (field.contains("v5l")) {
                        arch = Arch.ARMv5;
                    } else if (field.contains("v6l")) {
                        arch = Arch.ARMv6;
                    } else if (field.contains("v7l")) {
                        arch = Arch.ARMv7;
                    } else {
                        System.out.println("What is this?; " + field);
                    }
                }
            }

            // ARMv5 doesnt have anything we care about
            else if (field.startsWith("Features") && (arch == Arch.ARMv6 || arch == Arch.ARMv7)) {
                if (field.contains("neon")) { // v6 shouldnt have Neon.
                    haveNeon = true;
                }
                if (field.contains("vfp")) {
                    haveVFP = true;
                }
            }
        }

        System.out.println("Found " + arch.toString() + " neon: " + haveNeon + " vfp: " + haveVFP);

        detectionDone = true;
    }

    public static boolean isShittyCPU() {

        if (!detectionDone) {
            doCPUDetection();
        }

        if (arch != Arch.ARMv7) {
            return true;
        }

        return false;
    }

    public static boolean hasNeon() {

        if (!detectionDone) {
            doCPUDetection();
        }

        return haveNeon;

    }

    public static Arch getArch() {
        if (!detectionDone) {
            doCPUDetection();
        }

        return arch;
    }
}
