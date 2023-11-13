package net.oneandone.pommes.scm;

import net.oneandone.pommes.database.Gav;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.launcher.Failure;

public abstract class ScmUrl {
    private final Scm scm;

    public ScmUrl(Scm scm) {
        this.scm = scm;
    }

    public Scm scm() {
        return scm;
    }

    public abstract ScmUrl normalize();
    public abstract String directory();

    public boolean same(ScmUrl other) {
        return scm == other.scm && normalize().equals(other.normalize());
    }

    public void checkout(FileNode directory) throws Failure {
        scm.checkout(this, directory);
    }

    public String scmUrl() {
        return scm.protocol() + url();
    }

    public abstract String url();

    public abstract Gav defaultGav();

    public abstract int hashCode();
    public abstract boolean equals(Object obj);

}
