package net.oneandone.pommes.scm;

import net.oneandone.sushi.util.Strings;

import java.net.URI;

/**
 * Normalized git url.
 * Supports https and ssh schemes from https://git-scm.com/book/en/v2/Git-on-the-Server-The-Protocols,
 * including scp-like syntax.
 * See https://docs.github.com/en/get-started/getting-started-with-git/about-remote-repositories
 */
public class GitUrl {
    public static GitUrl create(String str) {
        if (str.contains("://")) {
            return regular(str);
        } else {
            return scplike(str);
        }
    }

    private static GitUrl scplike(String str) {
        int idx;
        URI uri;
        String path;

        idx = str.indexOf(':');
        if (idx < 1) {
            throw new IllegalArgumentException("invalid scp-like url: " + str);
        }
        uri = URI.create("ssh://" + str.substring(0, idx));
        if (!"git".equals(uri.getUserInfo())) {
            throw new IllegalArgumentException("git user expected in scp-like url: " + str);
        }
        path = str.substring(idx + 1);
        if (path.startsWith("/")) {
            throw new IllegalArgumentException("absolute path in scp-like url: " + str);
        }
        return new GitUrl(true, uri.getHost(), normalzeTail(path));
    }

    private static GitUrl regular(String str) {
        URI uri;
        String sshUser;
        String scheme;
        String path;

        uri = URI.create(str);
        scheme = uri.getScheme().toLowerCase();
        sshUser = uri.getUserInfo();
        if (sshUser == null) {
            if (!scheme.equals("https")) {
                throw new IllegalStateException("https scheme expected: " + str);
            }
        } else {
            if (!sshUser.equalsIgnoreCase("git")) {
                throw new IllegalStateException("unexpected user: " + str);
            }
            if (!scheme.equals("ssh")) {
                throw new IllegalStateException("ssh scheme expected: " + str);
            }
        }
        if (uri.getFragment() != null) {
            throw new IllegalStateException("unexpected fragment: " + str);
        }
        if (uri.getPort() != -1) {
            throw new IllegalStateException("unexecpted port: " + str);
        }
        path = uri.getPath();
        if (!path.startsWith("/")) {
            throw new IllegalArgumentException("absolute path expected: " + str);
        }
        path = path.substring(1);
        path = normalzeTail(path);
        return new GitUrl(sshUser != null, uri.getHost(), path);
    }

    private static String normalzeTail(String path) {
        path = Strings.removeRightOpt(path, "/");
        path = Strings.removeRightOpt(path, ".git");
        if (path.isEmpty()) {
            throw new IllegalArgumentException("empty path: " + path);
        }
        return path;
    }

    private final String host;
    private final boolean ssh; // null: https
    private final String path;  // path to project

    public GitUrl(boolean ssh, String host, String path) {
        this.ssh = ssh;
        this.host = host;
        this.path = path;
    }

    public boolean equiv(GitUrl other) {
        return host.equals(other.host) && path.equals(other.path);
    }

    public String getPath() {
        return path;
    }
    public String getHost() {
        return host;
    }
    public boolean ssh() {
        return ssh;
    }

    public GitUrl withSsh(boolean setSsh) {
        return new GitUrl(setSsh, host, path);
    }

    public int hashCode() {
        return path.hashCode();
    }

    public boolean equals(Object other) {
        if (other instanceof GitUrl giturl) {
            return ssh == giturl.ssh && host.equals(giturl.host) && path.equals(giturl.path);
        }
        return false;
    }

    public String url() {
        return (ssh ? "ssh://git@" : "https://") + host + "/" + path;
    }
}