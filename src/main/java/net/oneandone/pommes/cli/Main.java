/*
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
import net.oneandone.pommes.database.Field;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.util.Strings;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        World world;
        Cli cli;

        world = World.create();
        cli = Cli.create("Project checkout manager and database tool.\n"
                + "\n"
                + "Usage:\n"
                + "  'pommes' ['-v'|'-e'] command args*\n"
                + "\n"
                + "search commands\n"
                + "  'find' ('-output' str)? '-fold'? query ('-' format* | '-json' | '-dump' | '-'MACRO)? \n"
                + "                        print projects matching this query;\n"
                + "                        append '-json' to print json, '-dump' to print json without formatting;\n"
                + "                        format is a string with placeholders: %c is replace be the current checkout\n"
                + "                        and %FIELD_ID is replaced by the respective field;\n"
                + "                        place holders can be followed by angle brackets to filter for\n"
                + "                        the enclosed substring or variables;\n"
                + "                        output is a file or URL to write results to, default is the console.\n"
                + "\n"
                + "checkout commands\n"
                + "  'st' root?            print status of all checkouts under the specified directory:\n"
                + "                        ? - directory is not in database\n"
                + "                        M - checkout has un-tracked files, modifications or is not pushed\n"
                + "                        C - checkout url does not match the directory Pommes would place it in\n"
                + "  'goto' query          offer selection of matching projects, check it out when necessary,\n"
                + "                        and cds into the checkout directory\n"
                + "  'checkout' query      checkout matching projects; skips existing checkouts;\n"
                + "                        offers selection before changing anything on disk\n"
                + "  'remove' '-stale'? root?\n"
                + "                        remove (optional: stale) checkouts under the specified root directory;\n"
                + "                        a checkout is stale if the project has been removed from the database;\n"
                + "                        offers selection before changing anything on disk;\n"
                + "                        checkouts with uncommitted changes are marked in the list - example urls:\n"
                + "                            github:mlhartme\n"
                + "                            svn:https://svn.yourserver.org/some/path -skip/this/subpath"
                + "  'index'               (re-) builds the database.\n"
                + "\n"
                + "fields in the database: (field id is the first letter of the field name.)\n"
                + fieldList()
                + "\n"
                + "query syntax\n"
                + "  query     = '@' MACRO | or\n"
                + "  or        = (and (' ' and)*)? index?\n"
                + "  index     = NUMBER\n"
                + "  and       = term ('+' term)*\n"
                + "  term      = field | lucene\n"
                + "  field     = '!'? (FIELD_ID* match)? STR ; match on one of the specified fields (or 'ao' if not specified)\n"
                + "  match     = ':' | '^' | '%' | '='     ; substring, prefix, suffix or string match\n"
                + "  lucene    = '§' STR                   ; STR in Lucene query Syntax: "
                  + "https://lucene.apache.org/core/6_0_1/queryparser/org/apache/lucene/queryparser/classic/QueryParser.html\n"
                + "and STR may contain the following variables:\n"
                + "  {gav}     = coordinates for current project\n"
                + "  {ga}      = group and artifact of current project\n"
                + "  {scm}     = scm location for current directory\n"
                + "\n"
                + "environment:\n"
                + "  POMMES_HOME     home directory for Pommes\n"
                + "\n"
                + "Home: https://github.com/mlhartme/pommes\n");
        cli.primitive(FileNode.class, "file name", world.getWorking(), world::file);
        cli.begin(world);
          cli.add(Setup.class, "setup -batch");
          cli.begin(Environment.class);
            cli.add(Checkout.class, "checkout query*");
            cli.add(Remove.class, "remove -stale root?=.");

            cli.add(Status.class, "st root?=.");
            cli.add(Goto.class, "goto query*");

            cli.add(Index.class, "database-index");
            cli.add(Find.class, "find -output=null -fold queryOrFormat* { arg*(queryOrFormat)}");

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

    private Main() {
    }
}
