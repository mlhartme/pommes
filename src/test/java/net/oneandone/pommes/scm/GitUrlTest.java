package net.oneandone.pommes.scm;

import net.oneandone.pommes.scm.GitUrl;
import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GitUrlTest {
    @Test
    public void create() {
        // https protocol
        assertEquals("https://github.com/pustefix-projects/pustefix-framework", GitUrl.create("https://github.com/pustefix-projects/pustefix-framework.git").url());

        // ssh protocol
        assertEquals("ssh://git@github.com/mlhartme/maven-active-markdown-plugin", GitUrl.create("ssh://git@github.com/mlhartme/maven-active-markdown-plugin.git").url());
        assertEquals("github.com/jkschoen/jsma", GitUrl.create("git@github.com:jkschoen/jsma.git"));

        // TODO
        // assertEquals("github.com/tcurdt/jdeb", GitUrl.create("git://github.com:tcurdt/jdeb.git"));

    }
    @Test
    public void equiv() {
        GitUrl left = GitUrl.create("https://github.com/mlhartme/foo.git");
        GitUrl right = GitUrl.create("ssh://git@github.com/mlhartme/foo");
        assertTrue(left.equiv(right));
        assertEquals("https://github.com/mlhartme/foo", left.url());
        assertEquals("ssh://git@github.com/mlhartme/foo", right.url());
    }
}
