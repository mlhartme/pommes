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
import java.util.List;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.WildcardQuery;

import com.google.gson.Gson;

import net.oneandone.pommes.model.Database;
import net.oneandone.pommes.model.Pom;
import net.oneandone.sushi.fs.Node;

public class DatabaseExport extends Base {
    private final Gson gson = new Gson();
    private final String scmSubstring;
    private final String exportPath;

    public DatabaseExport(Globals globals, String scmSubstring, String exportPath) {
        super(globals);

        this.scmSubstring = scmSubstring;
        this.exportPath = exportPath;
    }

    @Override
    public void run(Database database) throws Exception {
        Node export = world.node(exportPath);
        console.verbose.println("Selecting all documents with " + scmSubstring + " in its origin");
        List<Pom> poms = database.query(new WildcardQuery(new Term(Database.ORIGIN, "*" + scmSubstring + "*")));
        console.verbose.println("Got " + poms.size() + " doucments");
        Node temp = world.getTemp().createTempFile();
        temp.writeLines(gson.toJson(poms));
        console.verbose.println("Wrote temp file " + temp.getPath());
        console.verbose.println("Saving to  " + export.getPath());
        temp.copyFile(export);
        console.verbose.println("Saving done.");

    }
}
