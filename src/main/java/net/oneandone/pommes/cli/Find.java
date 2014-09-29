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
import net.oneandone.pommes.model.Database;
import net.oneandone.pommes.model.Environment;
import net.oneandone.pommes.model.GAV;
import net.oneandone.pommes.model.Pom;
import net.oneandone.pommes.mount.Fstab;
import net.oneandone.pommes.mount.Point;
import net.oneandone.sushi.cli.Console;
import net.oneandone.sushi.cli.Option;
import net.oneandone.sushi.cli.Value;
import net.oneandone.sushi.fs.file.FileNode;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingException;

import java.io.IOException;
import java.util.List;

/**
 * Lists all indexed POMs in index *without* updating from the server. Warning: this will be a long list, only useful with grep.
 */
public class Find extends SearchBase<Pom> {
    private final Fstab fstab;

    public Find(Console console, Maven maven) throws IOException {
        super(console, maven);
        this.fstab = Fstab.load(console.world);
    }

    @Value(name = "query", position = 1)
    private String query;

    @Option("format")
    private String format = "%g @ %o %c";

    public List<Pom> search(Database database) throws IOException, QueryNodeException {
        return database.query(new Environment(console.world, maven), query);
    }

    @Override
    public Pom toPom(Pom pom) {
        return pom;
    }

    @Override
    public String toLine(Pom pom) {
        return format(pom);
    }

    private String format(Pom pom) {
        char c;
        StringBuilder result;
        String url;
        boolean first;
        String str;
        int end;
        String filter;

        result = new StringBuilder();
        for (int i = 0, max = format.length(); i < max; i++) {
            c = format.charAt(i);
            if (c == '%' && i + 1 < max) {
                i++;
                c = format.charAt(i);
                switch (c) {
                    case '%':
                        result.append(c);
                        break;
                    case 'g':
                        result.append(pom.coordinates.toGavString());
                        break;
                    case 'o':
                        result.append(pom.origin);
                        break;
                    case 'd':
                        if (i + 1 < max && format.charAt(i + 1) == '[') {
                            end = format.indexOf(']', i + 2);
                            if (end == -1) {
                                throw new IllegalStateException("invalid format: " + format);
                            }
                            filter = format.substring(i + 2, end);
                            i = end;
                            for (GAV dep : pom.dependencies) {
                                str = dep.toGavString();
                                if (str.contains(filter)) {
                                    result.append(str);
                                }
                            }
                        } else {
                            result.append(pom.dependencies.toString());
                        }
                        break;
                    case 'c':
                        first = true;
                        url = pom.projectUrl();
                        for (FileNode directory : fstab.directories(url)) {
                            if (directory.exists()) {
                                if (first) {
                                    first = false;
                                } else {
                                    result.append(' ');
                                }
                                result.append(directory.getAbsolute());
                            }
                        }
                        break;
                    default:
                        throw new IllegalStateException("invalid format character: " + c);
                }
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }
}
