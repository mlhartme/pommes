/**
 * Copyright 1&1 Internet AG, https://github.com/1and1/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
