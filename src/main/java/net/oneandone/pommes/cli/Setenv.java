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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class Setenv implements Runnable {
    private static Setenv lazySetenv;

    public static Setenv get() {
        if (lazySetenv == null) {
            lazySetenv = create();
        }
        return lazySetenv;
    }

    public static Setenv create() {
        String str;
        Setenv result;

        str = System.getenv("SETENV_BASE");
        result = new Setenv(new PrintWriter(System.out, true), str == null ? null : Paths.get(str + "-" + pid()));
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

    public Setenv(PrintWriter messages, Path dest) {
        this.messages = messages;
        this.dest = dest;
        this.lines = new ArrayList<>();
    }

    public boolean isConfigured() {
        return dest != null;
    }

    public String setenvBash() {
        byte[] buffer = new byte[2];
        InputStream src;
        ByteArrayOutputStream bytes;
        int count;

        src = getClass().getResourceAsStream("/setenv.bash");
        bytes = new ByteArrayOutputStream();
        while (true) {
            try {
                count = src.read(buffer);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
            if (count == -1) {
                try {
                    return new String(bytes.toByteArray(), "UTF8");
                } catch (UnsupportedEncodingException e) {
                    throw new IllegalStateException(e);
                }
            }
            bytes.write(buffer, 0, count);
        }
    }

    public void cd(String path) {
        line("cd " + escape(path));
    }

    public void set(String key, String value) {
        line("export " + key + "=" + escape(value) + "\n");
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
