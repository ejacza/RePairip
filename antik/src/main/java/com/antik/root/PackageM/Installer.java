package com.antik.root.PackageM;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

public class Installer {
    public static void install(File D_TOY) {
        try {
            System.out.println("[ROOT] Installing : " + D_TOY.getName());

            Process p = Runtime.getRuntime().exec(new String[]{
                    "su", "-c", "pm install -r -t -g " + D_TOY.getAbsolutePath()
            });

            BufferedReader _TRead = new BufferedReader(new InputStreamReader(p.getInputStream()));

            String LN;

            while ((LN = _TRead.readLine()) != null) {
                System.out.println("[PM] " + LN);
            }

            p.waitFor();

            System.out.println("[ROOT] Finished");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
