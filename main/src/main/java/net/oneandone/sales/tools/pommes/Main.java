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
package net.oneandone.sales.tools.pommes;

import net.oneandone.pommes.maven.Maven;
import net.oneandone.sushi.cli.Child;
import net.oneandone.sushi.cli.Cli;
import net.oneandone.sushi.cli.Command;
import net.oneandone.sushi.fs.World;

import java.io.IOException;

public class Main extends Cli implements Command {
    public static void main(String[] args) throws IOException {
        System.exit(new Main().run(args));
    }

    public Main() {
        super(new World());
    }

    private Maven maven() throws IOException {
        return Maven.withSettings(console.world);
    }

    @Child("create")
    public Create create() throws IOException {
        return new Create(console, maven());
    }

    @Child("find")
    public Find find() throws IOException {
        return new Find(console, maven());
    }

    @Child("users")
    public Users users() throws IOException {
        return new Users(console, maven());
    }

    @Child("mount")
    public Mount mount() throws IOException {
        return new Mount(console, maven());
    }

    @Child("umount")
    public Umount umount() throws IOException {
        return new Umount(console, maven());
    }

    @Child("remount")
    public Remount remout() throws IOException {
        return new Remount(console, maven());
    }

    @Child("status")
    public Status status() throws IOException {
        return new Status(console, maven());
    }

    @Child("update")
    public Update update() throws IOException {
        return new Update(console, maven());
    }

    @Override
    public void invoke() throws Exception {
        printHelp();
    }

    @Override
    public void printHelp() {
        console.info.println("Pom database. ");
        console.info.println();
        console.info.println("Usage: ");
        console.info.println("  'pommes' ['-v'|'-e'] command args*");
        console.info.println("search commands");
        console.info.println("  'find' substring      prints projects who's coordinates or scm url contains substring");
        console.info.println("  'users' gav?          prints projects that use the specified artifact (gav defaults to current project)");
        console.info.println("mount commands");
        console.info.println("  'mount' ('-match' str)? url dir? ");
        console.info.println("                        adds a new mount point with checkouts of all trunks under the specified url;");
        console.info.println("                        dir default to current directory;");
        console.info.println("                        asks before changing anything on your disk");
        console.info.println("  'umount' dir?         removes all mount points and checkouts under dir (default is current directory)");
        console.info.println("                        asks before changing anything on your disk");
        console.info.println("  'remount' dir?        checks all mount points under dir (default is current directory) for projects");
        console.info.println("                        added to - or removed from - the registry and add - or removes - checkouts;");
        console.info.println("                        asks before changing anything on your disk");
        console.info.println("svn commands");
        console.info.println("  'status' dir?         check checkouts under the specified directory against svn.");
        console.info.println("  'update' dir?         updates checkouts under the specified directory form svn");
        console.info.println("admin commands");
        console.info.println("  'create' '-global'? url*");
        console.info.println("                        creates a new registry containing the projects found under the specified urls;");
        console.info.println("                        use '-' pattern to exclude from the url before;");
        console.info.println("                        (urls default to all ciso urls if not specified)");
        console.info.println();
        console.info.println("environment:");
        console.info.println("  POMMES_GLOBAL         where to store the global database file.");
        console.info.println();
        console.info.println("Documentation: https://wiki.1and1.org/UE/PommesHome");
    }
}
