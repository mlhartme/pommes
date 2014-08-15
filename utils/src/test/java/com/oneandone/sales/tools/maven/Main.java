package com.oneandone.sales.tools.maven;

import net.oneandone.sushi.fs.World;
import org.apache.maven.project.MavenProject;

public final class Main {

    public static void main(String[] args) throws Exception {
        World world;
        Maven maven;
        MavenProject pom;

        world = new World();
        maven = Maven.withSettings(world);
        pom = maven.loadPom(world.file("/Users/mhm/Projects/VTRHOST.072-2/pom.xml"), false);
        System.out.println(pom.toString());
    }

    private Main() {
    }
}
