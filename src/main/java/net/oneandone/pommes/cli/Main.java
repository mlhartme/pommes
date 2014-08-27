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
package net.oneandone.pommes.cli;

import net.oneandone.maven.embedded.Maven;
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

    //--

    @Child("find")
    public Find find() throws IOException {
        return new Find(false, console, maven());
    }

    @Child("query")
    public Find query() throws IOException {
        return new Find(true, console, maven());
    }

    @Child("users")
    public Users users() throws IOException {
        return new Users(console, maven());
    }

    @Child("mount")
    public Mount mount() throws IOException {
        return new Mount(console, maven());
    }

    //--

    @Child("database-clear")
    public DatabaseClear clear() throws IOException {
        return new DatabaseClear(console, maven());
    }

    @Child("database-add")
    public DatabaseAdd add() throws IOException {
        return new DatabaseAdd(console, maven());
    }

    @Child("database-remove")
    public DatabaseRemove remove() throws IOException {
        return new DatabaseRemove(console, maven());
    }

    //--

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
        console.info.println("  'query' query         prints projects matching the specified lucene query");
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
        console.info.println("database commands");
        console.info.println("  'database-clear'");
        console.info.println("                        creates a new empty database");
        console.info.println("  'database-add' url*");
        console.info.println("                        adds projects found under the specified urls to the database;");
        console.info.println("                        use '-' pattern to exclude from the url before;");
        console.info.println("                        (urls default to all ciso urls if not specified)");
        console.info.println("  'database-remove' url*");
        console.info.println("                        removes all documents prefixed with one of the specified urls");
        console.info.println();
        console.info.println("update options (for all aearch and database commands:");
        console.info.println("  default behavior      one read update from global database per day, "
                + "no write updates to the global database");
        console.info.println("  '-global'             perform read/write update from/to global database");
        console.info.println("  '-local'              neither read- nor write updates from/to global database");
        console.info.println();
        console.info.println("environment:");
        console.info.println("  POMMES_GLOBAL         where to store the global database file.");
        console.info.println();
        console.info.println("Home: https://github.com/mlhartme/pommes");
    }
}
