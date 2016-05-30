package net.oneandone.pommes.scm;

import net.oneandone.inline.Console;
import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.launcher.Failure;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public abstract class Scm {
    private static final Subversion[] SCMS = {
        new Subversion()
    };

    public static void scanCheckouts(FileNode directory, Map<FileNode, Scm> result) throws IOException {
        List<FileNode> children;
        Scm probed;

        probed = probeCheckout(directory);
        if (probed != null) {
            result.put(directory, probed);
        } else {
            children = directory.list();
            if (children != null) {
                for (FileNode child : children) {
                    scanCheckouts(child, result);
                }
            }
        }
    }

    public static Scm probeCheckout(FileNode checkout) throws IOException {
        for (Scm scm : SCMS) {
            if (scm.isCheckout(checkout)) {
                return scm;
            }
        }
        return null;
    }

    public static Scm probeUrl(String url) throws IOException {
        for (Scm scm : SCMS) {
            if (scm.isUrl(url)) {
                return scm;
            }
        }
        return null;
    }


    //--

    public abstract boolean isCheckout(FileNode directory) throws IOException;
    public abstract boolean isCommitted(FileNode checkout) throws IOException;
    public abstract boolean exists(World world, String url) throws IOException;
    public abstract boolean isUrl(String url);
    public abstract String getUrl(FileNode checkout) throws Failure;
    public abstract void checkout(FileNode dir, String url, Console console) throws Failure;
}
