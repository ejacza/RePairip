package com.antik.root;

public class launchApp {
    public static void launchApp(String _Pname) {
        try {
            System.out.println("[ROOT] Launching " + _Pname);
            Runtime.getRuntime().exec(new String[]{
                            "su",
                            "-c",
                            "monkey -p " + _Pname + " -c android.intent.category.LAUNCHER 1"
                    }
            ).waitFor();

            System.out.println("[ROOT] Finished ");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
