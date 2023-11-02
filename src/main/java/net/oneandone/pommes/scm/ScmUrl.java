package net.oneandone.pommes.scm;

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
        return scm == other.scm &&  normalize().equals(other.normalize());
    }

    public String scmUrl() {
        return scm.protocol() + url();
    }

    public abstract String url();

}
