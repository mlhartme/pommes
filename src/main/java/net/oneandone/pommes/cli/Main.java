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

import net.oneandone.sushi.cli.Child;
import net.oneandone.sushi.cli.Cli;
import net.oneandone.sushi.cli.Command;
import net.oneandone.sushi.cli.Option;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.file.FileNode;

import java.io.IOException;

public class Main extends Cli implements Command {
    public static void main(String[] args) throws IOException {
        System.exit(new Main().run(args));
    }

    public Main() {
        super(new World());
    }

    private Environment env() throws IOException {
        return new Environment(console.world);
    }

    @Option("shellFile")
    private FileNode shellFile;

    //--

    @Child("find")
    public Find find() throws IOException {
        return new Find(console, env(), "", "%g @ %o %c");
    }

    @Child("users")
    public Find users() throws IOException {
        return new Find(console, env(), ":-=ga=+@trunk", "%g @ %o -> %d[%ga]");
    }

    //--

    @Child("mount")
    public Mount mount() throws IOException {
        return new Mount(console, env());
    }

    @Child("umount")
    public Umount umount() throws IOException {
        return new Umount(console, env());
    }

    @Child("list")
    public Lst list() throws IOException {
        return new Lst(console, env());
    }

    @Child("goto")
    public Goto goTo() throws IOException {
        return new Goto(console, env(), shellFile);
    }

    //--

    @Child("fstab-add")
    public FstabAdd fstabAdd() throws IOException {
        return new FstabAdd(console, env());
    }

    //--

    @Child("database-clear")
    public DatabaseClear clear() throws IOException {
        return new DatabaseClear(console, env());
    }

    @Child("database-add")
    public DatabaseAdd add() throws IOException {
        return new DatabaseAdd(console, env());
    }

    @Child("database-remove")
    public DatabaseRemove remove() throws IOException {
        return new DatabaseRemove(console, env());
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
        console.info.println("  'pommes' ['-v'|'-e'] command sync-options args*");
        console.info.println();
        console.info.println("search commands");
        console.info.println("  'find' ('-format' str)? query");
        console.info.println("                        print projects matching this query;");
        console.info.println("                        format string supports the following place holder:");
        console.info.println("                        %g gav   %o origin  %d dependencies  %c checkouts;");
        console.info.println("                        place holders can be followed by angle brackets to filter for");
        console.info.println("                        the enclosed substring or variable");
        console.info.println("  'users'");
        console.info.println("                        print projects that have a dependency to the current project;");
        console.info.println("                        same as 'pommes find -format \"%g @ %o -> %d[%ga]\" :-=ga=+@trunk ");
        console.info.println();
        console.info.println("mount commands");
        console.info.println("  'mount' query         checkout matching projects; skips existing checkouts;");
        console.info.println("                        offers selection before changing anything on disk;");
        console.info.println("  'umount' '-stale'? root?");
        console.info.println("                        remove (optional: stale) checkouts under the specified root directory;");
        console.info.println("                        a checkout is stale if the project has been removed from the database;");
        console.info.println("                        offers selection before changing anything on disk;");
        console.info.println("                        checks for uncommitted changes before selection");
        console.info.println("  'list' root?          print all checkouts under the specified directory with status markers:");
        console.info.println("                        C - checkout url does not match url configured by fstab");
        console.info.println("                        ? - checkout has no configured url configured by fstab");
        console.info.println("  'goto' query          offer selection of matching project, check it out when necessary, ");
        console.info.println("                        and cd into the checkout directory");
        console.info.println();
        console.info.println("database commands");
        console.info.println("  'database-clear'      creates a new empty database");
        console.info.println("  'database-add' url* '-noBranches'?");
        console.info.println("                        add projects found under the specified urls to the database;");
        console.info.println("                        overwrites projects with same origin;");
        console.info.println("                        use '-' pattern to exclude from the url before");
        console.info.println("  'database-remove' url*");
        console.info.println("                        remove all documents prefixed with one of the specified urls");
        console.info.println();
        console.info.println("other commands");
        console.info.println("  'fstab-add' url directory");
        console.info.println("                        add an entry to fstab; create directory if it does not exist");
        console.info.println();
        console.info.println("sync options            how to sync between global and local database");
        console.info.println("  default behavior      download global database once a day; no uploads");
        console.info.println("  '-download'           download global database before command execution");
        console.info.println("  '-noDownload'         no download of global database");
        console.info.println("  '-upload'             upload local database after command execution");
        console.info.println();
        console.info.println("query syntax");
        console.info.println("  query     = term ('+' term)* | lucene");
        console.info.println("  term      = substring | gav | dep | origin");
        console.info.println("  substring = STR             ; substring in gav OR origin");
        console.info.println("  gav       = ':' STR         ; substring match in groupId:artifactId:version");
        console.info.println("  dep       = ':-' STR        ; substring match in dependency or parent");
        console.info.println("  origin    = '@' STR         ; substring match in origin");
        console.info.println("  lucene    = '%' STR         ; Lucene Query Syntax");
        console.info.println("and STR may contain the following variables:");
        console.info.println("  =gav=     = coordinates for current project");
        console.info.println("  =ga=      = group and artifact of current project");
        console.info.println("  =svn=     = svn location for current directory");
        console.info.println();
        console.info.println("environment:");
        console.info.println("  POMMES_GLOBAL         url pointing to global database zip file");
        console.info.println();
        console.info.println("Home: https://github.com/mlhartme/pommes");
    }
}
