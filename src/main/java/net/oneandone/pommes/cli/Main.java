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

import net.oneandone.inline.Cli;
import net.oneandone.pommes.model.Field;
import net.oneandone.sushi.fs.NodeInstantiationException;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.util.Strings;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;

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
                + "  'find' ('-target' str)? query ('-' format* | '-json' | '-dump')? \n"
                + "                        print projects matching this query;\n"
                + "                        append '-json' to print json, '-dump' to print json without formatting;\n"
                + "                        format is a string with placeholders: %c is replace be the current checkout\n"
                + "                        and %FIELD_ID replaced by the respective field\n"
                + "                        place holders can be followed by angle brackets to filter for\n"
                + "                        the enclosed substring or variables;\n"
                + "                        target is a file or URL to write results to, default is the console\n"
                + "  'users'\n"
                + "                        print projects that have a dependency to the current project;\n"
                + "                        same as 'pommes find -format \"%a @ %s -> %d[%ga]\" :-=ga=+@trunk\n"
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
                + "                        and cds into the checkout directory\n"
                + "\n"
                + "commands that modify the database\n"
                + "  'database-clear'      creates a new empty database\n"
                + "  'database-add' '-dryrun? url*\n"
                + "                        add projects found under the specified urls to the database;\n"
                + "                        overwrites projects with same origin;\n"
                + "                        url is a svn url, artifactory url, an option (prefixed with '%') or an exclude (prefixed with '-')"
                + "  'database-remove' query\n"
                + "                        remove all matching documents\n"
                + "\n"
                + "fields in the database:\n"
                + fieldList()
                + "  (field id is the first letter of the field name.)\n"
                + "\n"
                + "sync options            how to sync between global and local database\n"
                + "  default behavior      download global database once a day; no uploads\n"
                + "  '-download'           download global database before command execution\n"
                + "  '-no-download'        no download of global database\n"
                + "  '-upload'             upload local database after command execution\n"
                + "\n"
                + "query syntax\n"
                + "  query     = '§' MACRO | or\n"
                + "  or        = (and (' ' and)*)? \n"
                + "  and       = term ('+' term)*\n"
                + "  term      = substring | field | lucene\n"
                + "  substring = STR                    ; substring in gav OR origin\n"
                + "  field     = FIELD_LETTER* ':' STR  ; substring match on one of the specified fields (or 'ao' if not specified)\n"
                + "  lucene    = '!' STR                ; STR in Lucene query Syntax: https://lucene.apache.org/core/6_0_1/queryparser/org/apache/lucene/queryparser/classic/QueryParser.html\n"
                + "and STR may contain the following variables:\n"
                + "  =gav=     = coordinates for current project\n"
                + "  =ga=      = group and artifact of current project\n"
                + "  =svn=     = svn location for current directory (according to fstab)\n"
                + "\n"
                + "examples:\n"
                + "  pommes find :foo+@trunk                   # projects with foo in they gav and trunk in their scm\n"
                + "  pommes find :-slf4j                       # projects with a dependency to slf4j\n"
                + "  pommes find -format %a :-slf4j            # same, but only print gav of the matches\n"
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
                + "  POMMES_MOUNT          directory where to mount projects\n"
                + "  POMMES_LOCAL          where to store the local (copy of the) database\n"
                + "  POMMES_GLOBAL         url pointing to global database zip file\n"
                + "\n"
                + "Home: https://github.com/mlhartme/pommes\n");
        cli.primitive(FileNode.class, "file name", world.getWorking(), world::file);
        cli.begin(world);
        cli.begin(Environment.class, "-download -no-download -upload");
          cli.add(Mount.class, "mount query*");
          cli.add(Umount.class, "umount -stale root?=.");

          cli.add(Ls.class, "list root?=.");
          cli.add(Goto.class, "goto -shellFile=" + ((FileNode) world.getHome().join(".pommes.goto")).getAbsolute() + " query*");

          cli.add(DatabaseClear.class, "database-clear");
          cli.add(DatabaseAdd.class, "database-add -dryrun url* { add*(url) }");
          cli.add(DatabaseRemove.class, "database-remove prefix*");

          cli.add(Find.class, "find -target=null queryOrFormat* { arg*(queryOrFormat)}");

        System.exit(cli.run(args));
    }

    private static String fieldList() {
        StringBuilder result;

        result = new StringBuilder();
        for (Field field : Field.values()) {
            result.append("  " + Strings.padRight(field.dbname(), 12) + field.description + "\n");
        }
        return result.toString();
    }

}
