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

import net.oneandone.pommes.model.Database;
import net.oneandone.pommes.model.GAV;
import net.oneandone.pommes.model.Pom;
import net.oneandone.sushi.cli.ArgumentException;
import net.oneandone.sushi.cli.Console;
import net.oneandone.sushi.cli.Option;
import net.oneandone.sushi.cli.Remaining;
import net.oneandone.sushi.fs.file.FileNode;

import java.io.IOException;
import java.util.List;

public class Find extends Base {
    private boolean explicitQuery;

    public Find(Console console, Environment environment, String defaultQuery, String defaultFormat) throws IOException {
        super(console, environment);
        this.format = defaultFormat;
        this.query = defaultQuery;
        this.explicitQuery = false;
    }

    private String query;

    @Remaining
    public void remaining(String str) {
        if (explicitQuery) {
            throw new ArgumentException("too many queries");
        }
        query = str;
        explicitQuery = true;
    }

    @Option("format")
    private String format;

    public void invoke(Database database) throws Exception {
        List<Pom> matches;

        matches = database.query(query, environment);
        for (Pom pom : matches) {
            console.info.println(format(pom));
        }
    }

    private String format(Pom pom) throws IOException {
        char c;
        StringBuilder result;
        String url;
        boolean first;
        String str;
        int end;
        String variable;
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
                            if (filter.startsWith("%")) {
                                variable = filter.substring(1);
                                filter = environment.lookup(variable);
                                if (filter == null) {
                                    throw new IllegalStateException("unknown variable in format: " + variable);
                                }
                            }
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
                        for (FileNode directory : environment.fstab().directories(url)) {
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
