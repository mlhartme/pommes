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
package net.oneandone.pommes.database;

import java.io.IOException;

public interface Variables {
    /** @return null when not found */
    String lookup(String var) throws IOException;

    String PREFIX = "{";
    String SUFFIX = "}";

    default String substitute(String string) throws IOException {
        StringBuilder builder;
        int start;
        int end;
        int last;
        String var;
        String replaced;

        builder = new StringBuilder();
        last = 0;
        while (true) {
            start = string.indexOf(PREFIX, last);
            if (start == -1) {
                if (last == 0) {
                    return string;
                } else {
                    builder.append(string.substring(last));
                    return builder.toString();
                }
            }
            end = string.indexOf(SUFFIX, start + PREFIX.length());
            if (end == -1) {
                throw new IllegalArgumentException("missing end marker");
            }
            var = string.substring(start + PREFIX.length(), end);
            replaced = this.lookup(var);
            if (replaced == null) {
                throw new IOException("undefined variable: " + var);
            }
            builder.append(string, last, start);
            builder.append(replaced);
            last = end + SUFFIX.length();
        }
    }


}
