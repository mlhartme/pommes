package net.oneandone.pommes.scm;

import net.oneandone.pommes.model.Gav;
import net.oneandone.sushi.launcher.Failure;
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

    @Test
    public void ssl() throws Failure {
        Git git;

        git = new Git();
        assertEquals(new Gav("cisoops", "clm", "1-SNAPSHOT"), git.defaultGav("ssh://git@bitbucket.1and1.org/cisoops/clm.git"));
    }
}
