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
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class AutoCd implements Runnable {
    private static AutoCd lazyAutoCd;

    public static AutoCd get() {
        if (lazyAutoCd == null) {
            lazyAutoCd = create();
        }
        return lazyAutoCd;
    }

    public static AutoCd create() {
        String str;
        AutoCd result;

        str = System.getenv("POMMES_AUTO_CD");
        result = new AutoCd(new PrintWriter(System.out, true), str == null ? null : Paths.get(str + "-" + pid()));
        Runtime.getRuntime().addShutdownHook(new Thread(result));
        return result;
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

    //--

    private PrintWriter messages;
    private final Path dest;
    private List<String> lines;

    public AutoCd(PrintWriter messages, Path dest) {
        this.messages = messages;
        this.dest = dest;
        this.lines = new ArrayList<>();
    }

    public boolean isConfigured() {
        return dest != null;
    }

    public void cd(String path) {
        line(escape(path));
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

    public void line(String line) {
        lines.add(line);
    }

    @Override
    public void run() {
        if (dest == null) {
            messages.print(toString());
        } else {
            try {
                save();
            } catch (IOException e) {
                e.printStackTrace(messages);
            }
        }
    }

    public void save() throws IOException {
        Files.write(dest, toString().getBytes("UTF8"));
    }

    public String toString() {
        StringBuilder result;

        result = new StringBuilder();
        for (String line : lines) {
            result.append(line).append('\n');
        }
        return result.toString();
    }
}
