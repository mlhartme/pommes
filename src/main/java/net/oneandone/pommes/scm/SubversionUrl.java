package net.oneandone.pommes.scm;

import net.oneandone.sushi.util.Strings;

import java.net.URI;
import java.net.URISyntaxException;

public class SubversionUrl extends ScmUrl {
    private final URI uri;
    public SubversionUrl(String url) throws URISyntaxException {
        super(Scm.SUBVERSION);
        this.uri = new URI(Strings.removeRightOpt(url, "/"));
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
}
