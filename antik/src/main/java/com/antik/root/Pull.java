package com.antik.root;

import java.io.File;

public class Pull {
    public static boolean waitForFileAndPull(String packageName, String internalPath, File destination) {
        String fullPath = "/data/data/" + packageName + "/" + internalPath;

        try {
            System.out.println("[ROOT] Waiting for: " + fullPath);

            long startTime = System.currentTimeMillis();
            long timeout = 60000;

            while (System.currentTimeMillis() - startTime < timeout) {
                Process checkProcess = Runtime.getRuntime().exec(
                        new String[]{
                                "su",
                                "-c",
                                "ls " + fullPath
                        }
                );

                if (checkProcess.waitFor() == 0) {
                    System.out.println("[ROOT] File found.");

                    String cmd = "cp " + fullPath + " " + destination.getAbsolutePath() + " && chmod 666 " + destination.getAbsolutePath();

                    Process C_pad = Runtime.getRuntime().exec(
                            new String[]{
                                    "su",
                                    "-c",
                                    cmd
                            }
                    );

                    C_pad.waitFor();

                    System.out.println("[ROOT] Pulled successfully");
                    return true;
                }

                Thread.sleep(2000);
            }

            System.err.println("[ERROR] Timeout ");

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }
}
