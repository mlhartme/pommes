package net.oneandone.pommes.scm;

import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GitUrlTest {
    @Test
    public void create() throws ScmUrlException {
        // https protocol
        assertEquals("https://github.com/pustefix-projects/pustefix-framework", GitUrl.create("https://github.com/pustefix-projects/pustefix-framework.git").url());

        // ssh protocol
        assertEquals("ssh://git@github.com/mlhartme/maven-active-markdown-plugin", GitUrl.create("ssh://git@github.com/mlhartme/maven-active-markdown-plugin.git").url());

        // scp like
        assertEquals("ssh://git@github.com/jkschoen/jsma", GitUrl.create("git@github.com:jkschoen/jsma.git").url());
        var e = assertThrows(ScmUrlException.class, () -> GitUrl.create("github.com:jkschoen/jsma.git").url());
        assertTrue(e.getMessage().contains("git user expected"));

        // TODO
        // assertEquals("github.com/tcurdt/jdeb", GitUrl.create("git://github.com:tcurdt/jdeb.git"));

    }
    @Test
    public void equiv() throws ScmUrlException {
        GitUrl left = GitUrl.create("https://github.com/mlhartme/foo.git");
        GitUrl right = GitUrl.create("ssh://git@github.com/mlhartme/foo");
        assertTrue(left.equiv(right));
        assertEquals("https://github.com/mlhartme/foo", left.url());
        assertEquals("ssh://git@github.com/mlhartme/foo", right.url());
    }
}
