package net.oneandone.pommes.scm;

import net.oneandone.inline.Console;
import net.oneandone.sushi.fs.ExistsException;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.launcher.Failure;
import net.oneandone.sushi.launcher.Launcher;
import net.oneandone.sushi.util.Separator;
import net.oneandone.sushi.util.Strings;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

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
    public boolean exists(World world, String url) throws IOException {
        throw new IllegalStateException();
    }

    @Override
    public boolean isCommitted(FileNode directory) throws IOException {
        throw new IllegalStateException();
    }

    private static Launcher git(FileNode dir, String ... args) {
        Launcher launcher;

        launcher = new Launcher(dir);
        launcher.arg("git");
        launcher.arg(args);
        return launcher;
    }
}
