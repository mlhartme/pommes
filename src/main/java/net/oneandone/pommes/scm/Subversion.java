package net.oneandone.pommes.scm;

import net.oneandone.inline.Console;
import net.oneandone.sushi.fs.ExistsException;
import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.launcher.Failure;
import net.oneandone.sushi.launcher.Launcher;
import net.oneandone.sushi.util.Separator;
import net.oneandone.sushi.util.Strings;

import java.io.IOException;

public class Subversion {
    public static boolean isCheckout(Node directory) throws ExistsException {
        return directory.join(".svn").isDirectory();
    }

    public static boolean notCommitted(FileNode directory) throws IOException {
        return notCommitted(directory.exec("svn", "status"));
    }

    private static boolean notCommitted(String lines) {
        for (String line : Separator.on("\n").split(lines)) {
            if (line.trim().length() > 0) {
                if (line.startsWith("X") || line.startsWith("Performing status on external item")) {
                    // needed for pws workspace svn:externals  -> ok
                } else {
                    return true;
                }
            }
        }
        return false;
    }

    public static void checkout(FileNode directory, String scm, Console console) throws Failure {
        String url;
        Launcher svn;

        url = Strings.removeLeft(scm, "svn:"); // TODO
        svn = Subversion.svn(directory.getParent(), "co", url, directory.getName());
        if (console.getVerbose()) {
            console.verbose.println(svn.toString());
        } else {
            console.info.println("svn co " + url + " " + directory.getAbsolute());
        }
        if (console.getVerbose()) {
            svn.exec(console.verbose);
        } else {
            // exec into string (and ignore it) - otherwise, Failure Exceptions cannot contains the output
            svn.exec();
        }
    }

    private static Launcher svn(FileNode dir, String ... args) {
        Launcher launcher;

        launcher = new Launcher(dir);
        launcher.arg("svn");
        launcher.arg("--non-interactive");
        launcher.arg("--trust-server-cert"); // needs svn >= 1.6
        launcher.arg(args);
        return launcher;
    }
}
