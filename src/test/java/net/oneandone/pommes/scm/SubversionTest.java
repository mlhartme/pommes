package net.oneandone.pommes.scm;

import net.oneandone.pommes.model.Gav;
import net.oneandone.sushi.launcher.Failure;
import org.junit.Test;

import java.net.URISyntaxException;

import static org.junit.Assert.assertEquals;

public class SubversionTest {
    @Test
    public void dflt() throws Failure, URISyntaxException {
        Subversion svn;

        svn = new Subversion();
        assertEquals(new Gav("", "puppet_ciso", "1-SNAPSHOT"), svn.defaultGav("svn:https://svn.1and1.org/svn/puppet_ciso/"));
    }
}
