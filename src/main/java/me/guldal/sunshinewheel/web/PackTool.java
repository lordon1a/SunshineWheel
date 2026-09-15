package me.guldal.sunshinewheel.web;

import java.io.File;

/**
 * Build-time helper: writes the standalone resource pack without a server.
 * Single source of truth stays in {@link PackServer}.
 */
public final class PackTool {

    private PackTool() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            throw new IllegalArgumentException("Usage: PackTool <out.zip>");
        }
        File out = new File(args[0]);
        if (out.getParentFile() != null) {
            out.getParentFile().mkdirs();
        }
        PackServer.buildPack(out);
        System.out.println("pack written: " + out + " (" + out.length() + " bytes)");
    }
}
