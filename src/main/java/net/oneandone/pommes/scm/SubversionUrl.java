package net.oneandone.pommes.scm;

import net.oneandone.pommes.database.Gav;
import net.oneandone.sushi.util.Strings;

import java.net.URI;
import java.net.URISyntaxException;

public class SubversionUrl extends ScmUrl {
    private final URI uri;
    public SubversionUrl(String url) throws ScmUrlException {
        super(Scm.SUBVERSION);
        try {
            this.uri = new URI(Strings.removeRightOpt(url, "/"));
        } catch (URISyntaxException e) {
            throw new ScmUrlException(e.getInput(), e.getReason());
        }
    }

    @Override
    public ScmUrl normalize() {
        return this;
    }

    public String directory() {
        final String trunk = "/trunk";
        final String branches = "/branches/";
        String path;
        int idx;

        path = uri.getPath();
        path = Strings.removeLeftOpt(path, "/svn");
        if (path.endsWith(trunk)) {
            path = Strings.removeRight(path, trunk);
        } else {
            idx = path.lastIndexOf('/');
            if (idx != -1) {
                idx = path.lastIndexOf('/', idx - 1);
                if (idx != -1 && path.substring(idx).startsWith(branches)) {
                    path = path.substring(0, idx);
                }
            }
        }
        return uri.getHost() + path;
    }

    @Override
    public String url() {
        return uri.toString();
    }

    @Override
    public Gav defaultGav() {
        String path;
        int idx;
        String groupId;
        String artifactId;

        path = uri.getPath();
        path = Strings.removeRightOpt(path, "/");
        path = Strings.removeLeftOpt(path, "/svn");
        path = Strings.removeLeftOpt(path, "/");
        idx = path.indexOf("/trunk");
        if (idx == -1) {
            idx = path.indexOf("/branches");
        }
        if (idx != -1) {
            path = path.substring(0, idx);
        }
        idx = path.lastIndexOf('/');
        if (idx == -1) {
            groupId = "";
            artifactId = path;
        } else {
            groupId = path.substring(0, idx).replace('/', '.');
            artifactId = path.substring(idx + 1);
        }
        return new Gav(groupId, artifactId, "1-SNAPSHOT");
    }

    @Override
    public int hashCode() {
        return uri.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof SubversionUrl other) {
            return uri.equals(other.uri);
        }
        return false;
    }
}
