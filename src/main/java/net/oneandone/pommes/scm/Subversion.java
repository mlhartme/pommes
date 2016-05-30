package net.oneandone.pommes.scm;

import net.oneandone.inline.Console;
import net.oneandone.pommes.model.Database;
import net.oneandone.sushi.fs.ExistsException;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.launcher.Failure;
import net.oneandone.sushi.launcher.Launcher;
import net.oneandone.sushi.util.Separator;
import net.oneandone.sushi.util.Strings;

import java.io.IOException;

public class Subversion extends Scm {
    private static final String PROTOCOL = "svn:";

    public Subversion() {
    }

    public boolean isCheckout(FileNode directory) throws ExistsException {
        return directory.join(".svn").isDirectory();
    }

    public boolean isUrl(String url) {
        return url.startsWith(PROTOCOL);
    }

    @Override
    public String getUrl(FileNode checkout) throws Failure {
        String url;
        int idx;

        url = checkout.launcher("svn", "info").exec();
        idx = url.indexOf("URL: ") + 5;
        return withSlash(url.substring(idx, url.indexOf("\n", idx)));
    }

    public static String withSlash(String url) {
        if (!url.endsWith("/")) {
            url = url + "/";
        }
        return url;
    }

    @Override
    public void checkout(FileNode directory, String fullurl, Console console) throws Failure {
        String url;
        Launcher svn;

        url = Strings.removeLeft(fullurl, PROTOCOL);
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

    @Override
    public boolean exists(World world, String url) throws IOException {
        try {
            // TODO: could also be a network error ...
            svn(world.getWorking(), "ls", Strings.removeLeft(url, PROTOCOL)).exec();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public boolean isCommitted(FileNode directory) throws IOException {
        return isCommitted(directory.exec("svn", "status"));
    }

    private static boolean isCommitted(String lines) {
        for (String line : Separator.on("\n").split(lines)) {
            if (line.trim().length() > 0) {
                if (line.startsWith("X") || line.startsWith("Performing status on external item")) {
                    // needed for pws workspace svn:externals  -> ok
                } else {
                    return false;
                }
            }
        }
        return true;
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
