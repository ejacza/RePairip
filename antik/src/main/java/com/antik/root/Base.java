package com.antik.root;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

public class Base {
    public static void replaceBase(String _Pname, File Org) {
        try {
            System.out.println("[ROOT] Finding base.apk path {/}");

            Process P_Proc = Runtime.getRuntime().exec( new String[]{
                            "su",
                            "-c",
                            "pm path " + _Pname
                    }
            );

            BufferedReader ___Read = new BufferedReader(new InputStreamReader(P_Proc.getInputStream()));

            String line = ___Read.readLine();
            P_Proc.waitFor();

            if (line == null || !line.startsWith("package:")) {
                System.err.println("[ERROR] Package path not found : " + _Pname);
                return;
            }

            String installedPath = line.substring(8).trim();

            System.out.println("[ROOT] Replacing : " + installedPath);

            Runtime.getRuntime().exec(new String[]{
                            "su",
                            "-c",
                            "am force-stop " + _Pname
                    }
            ).waitFor();

            String cmd =  "cp " + Org.getAbsolutePath() + " " + installedPath + " && chmod 644 " + installedPath + " && chown system:system " + installedPath;

            Process replaceProcess = Runtime.getRuntime().exec( new String[]{
                            "su",
                            "-c",
                            cmd
                    }
            );

            replaceProcess.waitFor();

            System.out.println("[ROOT] Finished ");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
