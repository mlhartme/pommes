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

import java.io.IOException;

import net.oneandone.inline.Cli;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.file.FileNode;

public class Main {
    public static void main(String[] args) throws IOException {
        World world;
        Cli cli;

        world = World.create();
        cli = Cli.create("Project database tool.\n"
                + "\n"
                + "Usage:\n"
                + "  'pommes' ['-v'|'-e'] command sync-options args*\n"
                + "\n"
                + "search commands\n"
                + "  'find' ('-format' str)? query\n"
                + "                        print projects matching this query;\n"
                + "                        format string supports the following place holder:\n"
                + "                        %g gav   %o origin  %d dependencies  %c checkouts;\n"
                + "                        place holders can be followed by angle brackets to filter for\n"
                + "                        the enclosed substring or variables\n"
                + "  'users'\n"
                + "                        print projects that have a dependency to the current project;\n"
                + "                        same as 'pommes find -format \"%g @ %o -> %d[%ga]\" :-=ga=+@trunk\n"
                + "\n"
                + "mount commands\n"
                + "  'mount' query         checkout matching projects; skips existing checkouts;\n"
                + "                        offers selection before changing anything on disk;\n"
                + "  'umount' '-stale'? root?\n"
                + "                        remove (optional: stale) checkouts under the specified root directory;\n"
                + "                        a checkout is stale if the project has been removed from the database;\n"
                + "                        offers selection before changing anything on disk;\n"
                + "                        checks for uncommitted changes before selection\n"
                + "  'list' root?          print all checkouts under the specified directory with status markers:\n"
                + "                        C - checkout url does not match url configured by fstab\n"
                + "                        ? - checkout has no configured url configured by fstab\n"
                + "  'goto' query          offer selection of matching project, check it out when necessary,\n"
                + "                        and cd into the checkout directory\n"
                + "\n"
                + "database commands\n"
                + "  'database-clear'      creates a new empty database\n"
                + "  'database-add' '-dryrun? url*\n"
                + "                        add projects found under the specified urls to the database;\n"
                + "                        overwrites projects with same origin;\n"
                + "                        url is a svn url, artifactory url, an option (prefixed with '%') or an exclude (prefixed with '-')"
                + "  'database-remove' query\n"
                + "                        remove all matching documents\n"
                + "  'database-dump' '-pp'? query? target?\n"
                + "                        writes a filtered list of matching (default: all) documents in json-format to the target (or console)\n"
                + "\n"
                + "other commands\n"
                + "  'fstab-add' url directory\n"
                + "                        add an entry to fstab; create directory if it does not exist\n"
                + "\n"
                + "sync options            how to sync between global and local database\n"
                + "  default behavior      download global database once a day; no uploads\n"
                + "  '-download'           download global database before command execution\n"
                + "  '-no-download'        no download of global database\n"
                + "  '-upload'             upload local database after command execution\n"
                + "\n"
                + "query syntax\n"
                + "  query     = term ('+' term)* | lucene\n"
                + "  term      = substring | gav | dep | origin\n"
                + "  substring = STR             ; substring in gav OR origin\n"
                + "  gav       = ':' STR         ; substring match in groupId:artifactId:version\n"
                + "  dep       = ':-' STR        ; substring match in dependency or parent\n"
                + "  origin    = '@' STR         ; substring match in origin\n"
                + "  lucene    = '%' STR         ; STR in Lucene Query Syntax\n"
                + "and STR may contain the following variables:\n"
                + "  =gav=     = coordinates for current project\n"
                + "  =ga=      = group and artifact of current project\n"
                + "  =svn=     = svn location for current directory (according to fstab)\n"
                + "\n"
                + "examples:\n"
                + "  pommes find :foo+@trunk                   # projects with foo in they gav and trunk in their origin\n"
                + "  pommes find :-slf4j                       # projects with a dependency to slf4j\n"
                + "  pommes find -format %g :-slf4j            # same, but only print gav of the matches\n"
                + "  pommes find -format %d[slf4j] :-slf4j     # same, but only print the matched dependency\n"
                + "\n"
                + "  # add poms to the database:\n"
                + "  # - your local repository:\n"
                + "  pommes database-add ~/.m2/repository\n"
                + "  # - from subversion:\n"
                + "  pommes database-add 'svn:https://user:password@svn.yourcompany.com/your/repo/path'\n"
                + "  # - from artifactory:\n"
                + "  pommes database-add 'artifactory:https://user:password@artifactory.yourcompany.com/your/repo/path'\n"
                + "  # - from your local disk:\n"
                + "  pommes database-add file:///path/to/my/projects\n"
                + "\n"
                + "environment:\n"
                + "  POMMES_GLOBAL         url pointing to global database zip file\n"
                + "\n"
                + "Home: https://github.com/mlhartme/pommes\n");
        cli.primitive(FileNode.class, "file name", world.getWorking(), world::file);
        cli.begin(world);
        cli.begin(Environment.class, "-svnuser -svnpassword -download -no-download -upload");
          cli.add(Mount.class, "mount query");
          cli.add(Umount.class, "umount -stale root?=.");

          cli.add(Ls.class, "list root?=.");
          cli.add(Goto.class, "goto -shellFile=" + ((FileNode) world.getHome().join(".pommes.goto")).getAbsolute() + " query");

          cli.add(FstabAdd.class, "fstab-add url directory");

          cli.add(DatabaseClear.class, "database-clear");
          cli.add(DatabaseAdd.class, "database-add -dryrun url* { add*(url) }");
          cli.add(DatabaseRemove.class, "database-remove prefix*");
          cli.add(DatabaseDump.class, "database-dump -pp query? target?");

          cli.add(Find.class, "find -format=%g§20@§20%o§20%c query");
          cli.add(FindUsers.class, "users -format=%g§20@§20%o§20->§20%d[%ga]");

        System.exit(cli.run(args));
    }

    public static class FindUsers extends Find {
        public FindUsers(Environment environment, String format) {
            super(environment, format, ":-=ga=+@trunk");
        }
    }
}
