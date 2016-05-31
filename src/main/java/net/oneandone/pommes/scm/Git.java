package net.oneandone.pommes.scm;

import net.oneandone.sushi.fs.ExistsException;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.launcher.Failure;
import net.oneandone.sushi.launcher.Launcher;
import net.oneandone.sushi.util.Strings;

import java.io.IOException;

public class Git extends Scm {
    private static final String PROTOCOL = "git:";

    public Git() {
    }

    public boolean isCheckout(FileNode directory) throws ExistsException {
        return directory.join(".git").isDirectory();
    }

    public boolean isUrl(String url) {
        return url.startsWith(PROTOCOL);
    }

    @Override
    public String getUrl(FileNode checkout) throws Failure {
        return git(checkout, "config", "--get", "remote.origin.url").exec().trim();
    }

    @Override
    public Launcher checkout(FileNode directory, String fullurl) throws Failure {
        String url;

        url = Strings.removeLeft(fullurl, PROTOCOL);
        return git(directory.getParent(), "clone", url, directory.getName());
    }

    @Override
    public boolean isAlive(FileNode checkout) throws IOException {
        try {
            git(checkout, "fetch", "--dry-run").exec();
            return true;
        } catch (Failure e) {
            // TODO: detect other failures
            return false;
        }
    }

    @Override
    public boolean isCommitted(FileNode checkout) throws IOException {
        try {
            git(checkout, "diff", "--quiet").execNoOutput();
        } catch (Failure e) {
            return false;
        }
        try {
            git(checkout, "diff", "--cached", "--quiet").execNoOutput();
        } catch (Failure e) {
            return false;
        }

        //  TODO: other branches
        try {
            git(checkout, "diff", "@{u}..HEAD", "--quiet").execNoOutput();
        } catch (Failure e) {
            return false;
        }
        return true;
    }

    private static Launcher git(FileNode dir, String ... args) {
        Launcher launcher;

        launcher = new Launcher(dir);
        launcher.arg("git");
        launcher.arg(args);
        return launcher;
    }
}
