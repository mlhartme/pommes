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

import net.oneandone.inline.Console;
import net.oneandone.maven.summon.api.Config;
import net.oneandone.maven.summon.api.Maven;
import net.oneandone.pommes.database.Project;
import net.oneandone.pommes.database.Variables;
import net.oneandone.pommes.descriptor.Descriptor;
import net.oneandone.pommes.scm.Scm;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.fs.filter.Filter;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class Environment implements Variables {
    private final Console console;
    private final World world;
    public final Lib lib;
    private Maven lazyMaven;
    private Project lazyCurrentPom;
    private Filter lazyExcludes;

    public Environment(Console console, World world) throws IOException {
        this.console = console;
        this.world = world;
        this.lib = Lib.load(world);
        console.verbose.println("default storage: " + lib.properties().defaultStorage);
        this.lazyMaven = null;
        this.lazyCurrentPom = null;
        this.lazyExcludes = null;
    }

    public Filter excludes() {
        if (lazyExcludes == null) {
            lazyExcludes = new Filter();
            lazyExcludes.include("**/.idea");
            lazyExcludes.include(Lib.DIR);
        }
        return lazyExcludes;
    }

    public Console console() {
        return console;
    }

    public World world() {
        return world;
    }

    public Maven maven() throws IOException {
        if (lazyMaven == null) {
            Config config = new Config();
            config.allowExtensions().allowAll();
            config.allowPomRepositories().allowAll();
            lazyMaven = config.build();
        }
        return lazyMaven;
    }

    //-- Variables interface

    private Map<String, String> arguments = new HashMap<>();

    public void defineArgument(int i, String argument) {
        arguments.put(Integer.toString(i), argument);
    }

    public void clearArguments() {
        arguments.clear();
    }

    /* variable lookup */
    public String lookup(String name) throws IOException {
        switch (name) {
            case "scm":
                return currentPom().scm.scmUrl();
            case "gav":
                return currentPom().artifact.toGavString();
            case "ga":
                return currentPom().artifact.toGaString();
            default:
                return arguments.get(name);
        }
    }

    //--

    private Project currentPom() throws IOException {
        if (lazyCurrentPom == null) {
            lazyCurrentPom = scanPom(world.getWorking());
        }
        return lazyCurrentPom;
    }
    private Project scanPom(FileNode directory) throws IOException {
        Scm scm;
        Descriptor descriptor;

        scm = Scm.probeCheckout(directory);
        if (scm != null) {
            for (FileNode child : directory.list()) {
                Descriptor.Creator m = Descriptor.match(child.getName());
                if (m != null) {
                    descriptor = m.create(this, child, "unused", child.getPath(), child.sha(), scm.getUrl(directory));
                    return descriptor.load();
                }
            }
        }
        throw new IllegalStateException(directory.toString());
    }

    //--

    public static boolean cd(FileNode dir) throws IOException {
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
}
