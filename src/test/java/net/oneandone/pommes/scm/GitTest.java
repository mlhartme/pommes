package net.oneandone.pommes.scm;

import org.junit.Test;

import java.net.URISyntaxException;

import static org.junit.Assert.assertEquals;

public class GitTest {
    @Test
    public void server() throws URISyntaxException {
        Git git;

        git = new Git();
        assertEquals("github.com", git.server("git:https://github.com/pustefix-projects/pustefix-framework.git"));
        assertEquals("github.com", git.server("git:ssh://git@github.com/mlhartme/maven-active-markdown-plugin.git"));
        assertEquals("github.com", git.server("git:git://github.com:tcurdt/jdeb.git"));
        assertEquals("github.com", git.server("git:git@github.com:jkschoen/jsma.git"));
    }
}
