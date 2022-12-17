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

import net.oneandone.sushi.fs.file.FileNode;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class AutoCd {
    public static boolean set(FileNode dir) throws IOException {
        String str;
        Path dest;

        str = System.getenv("POMMES_AUTO_CD");
        if (str == null) {
            return false;
        }
        dest = Paths.get(str + "-" + pid());
        Files.write(dest, escape(dir.getAbsolute()).getBytes(Charset.defaultCharset()));
        return true;
    }

    public static int pid() {
        String str;
        int idx;

        str = ManagementFactory.getRuntimeMXBean().getName();
        idx = str.indexOf('@');
        if (idx != -1) {
            str = str.substring(0, idx);
        }
        return Integer.parseInt(str);
    }

    public static String escape(String str) {
        StringBuilder result;
        char c;

        result = new StringBuilder();
        for (int i = 0, max = str.length(); i < max; i++) {
            c = str.charAt(i);
            // see http://pubs.opengroup.org/onlinepubs/009604499/utilities/xcu_chap02.html, 2.2 Quoting
            switch (c) {
                case '|':
                case '&':
                case ';':
                case '<':
                case '>':
                case '(':
                case ')':
                case '$':
                case '`':
                case '\\':
                case '"':
                case '\'':
                case ' ':
                case '\t':
                case '\n':

                case '*':
                case '?':
                case '[':
                case '#':
                case '~':
                case '=':
                case '%':
                    result.append('\\').append(c);
                    break;
                default:
                    result.append(c);
            }
        }
        return result.toString();
    }

    private AutoCd() {
    }
}
