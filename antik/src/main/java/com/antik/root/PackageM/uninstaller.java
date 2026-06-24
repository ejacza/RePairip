package com.antik.root.PackageM;

public class uninstaller {

    public static void uninstall(String _Pname) {
        try {
            System.out.println("[ROOT] Uninstalling: " + _Pname);
            Process p = Runtime.getRuntime().exec(new String[]{"su", "-c", "pm uninstall " + _Pname});
            p.waitFor();
            System.out.println("[ROOT] Finished");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}