package net.oneandone.pommes.scm;

public record ScmUrl(Scm scm, Object url) {
    public boolean same(ScmUrl other) {
        if (scm == other.scm) {
            return scm.normalize(url).equals(scm.normalize(other.url));
        }
        return false;
    }
    public String scmUrl() {
        return scm.protocol() + url.toString();
    }
}
