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
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.util.Strings;

import java.io.IOException;

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
                + "  'find' ('-output' str)? query ('-' format* | '-json' | '-dump' | '-'MACRO)? \n"
                + "                        print projects matching this query;\n"
                + "                        append '-json' to print json, '-dump' to print json without formatting;\n"
                + "                        format is a string with placeholders: %c is replace be the current checkout\n"
                + "                        and %FIELD_ID is replaced by the respective field;\n"
                + "                        place holders can be followed by angle brackets to filter for\n"
                + "                        the enclosed substring or variables;\n"
                + "                        output is a file or URL to write results to, default is the console.\n"
                + "\n"
                + "mount commands\n"
                + "  'mount' query         checkout matching projects; skips existing checkouts;\n"
                + "                        offers selection before changing anything on disk;\n"
                + "  'umount' '-stale'? root?\n"
                + "                        remove (optional: stale) checkouts under the specified root directory;\n"
                + "                        a checkout is stale if the project has been removed from the database;\n"
                + "                        offers selection before changing anything on disk;\n"
                + "                        checks for uncommitted changes before selection\n"
                + "  'ls' root?            print all checkouts under the specified directory with status markers:\n"
                + "                        M - checkout has modifications or is not pushed\n"
                + "                        C - checkout url does not match the directory pommes would place it in\n"
                + "                        X - checkout has is unknown to pommes\n"
                + "                        ? - directory is not a checkout\n"
                + "  'goto' query          offer selection of matching project, check it out when necessary,\n"
                + "                        and cds into the checkout directory\n"
                + "\n"
                + "commands that modify the database\n"
                + "  'database-clear'      deletes the current database and creates a new empty one.\n"
                + "  'database-add' '-delete'? '-dryrun'? '-fixscm'? ('-zone' zone)?  url*\n"
                + "                        add projects found under the specified urls to the database;\n"
                + "                        zone is a prefix added to the id, it defaults to local;\n"
                + "                        overwrites projects with same origin;\n"
                + "                        url is a svn url, artifactory url, an option (prefixed with '%') or an exclude (prefixed with '-')"
                + "  'database-remove' query\n"
                + "                        remove all matching documents\n"
                + "\n"
                + "fields in the database:\n"
                + fieldList()
                + "  (field id is the first letter of the field name.)\n"
                + "\n"
                + "import options          how to handle configured imports\n"
                + "  default behavior      import once a day\n"
                + "  '-import'             import before command execution\n"
                + "  '-no-import'          no import\n"
                + "\n"
                + "query syntax\n"
                + "  query     = '@' MACRO | or\n"
                + "  or        = (and (' ' and)*)? \n"
                + "  and       = term ('+' term)*\n"
                + "  term      = field | lucene\n"
                + "  field     = (FIELD_LETTER* match)? STR ; match on one of the specified fields (or 'ao' if not specified)\n"
                + "  match     = ':' | '^' | ',' | '='     ; substring, prefix, suffix or string match\n"
                + "  lucene    = '!' STR                   ; STR in Lucene query Syntax: https://lucene.apache.org/core/6_0_1/queryparser/org/apache/lucene/queryparser/classic/QueryParser.html\n"
                + "and STR may contain the following variables:\n"
                + "  {gav}     = coordinates for current project\n"
                + "  {ga}      = group and artifact of current project\n"
                + "  {scm}     = scm location for current directory\n"
                + "\n"
                + "environment:\n"
                + "  POMMES_PROPERTIES     where to find the properties file\n"
                + "\n"
                + "Home: https://github.com/mlhartme/pommes\n");
        cli.primitive(FileNode.class, "file name", world.getWorking(), world::file);
        cli.begin(world);
        cli.begin(Environment.class, "-import -no-import");
          cli.add(Mount.class, "mount query*");
          cli.add(Umount.class, "umount -stale root?=.");

          cli.add(Ls.class, "ls root?=.");
          cli.add(Goto.class, "goto -shellFile=" + world.getHome().join(".pommes.goto").getAbsolute() + " query*");

          cli.add(DatabaseClear.class, "database-clear");
          cli.add(DatabaseAdd.class, "database-add -delete -dryrun -fixscm -zone=local url* { add*(url) }");
          cli.add(DatabaseRemove.class, "database-remove prefix*");

          cli.add(Find.class, "find -output=null queryOrFormat* { arg*(queryOrFormat)}");

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
