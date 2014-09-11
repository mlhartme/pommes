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

import net.oneandone.sushi.cli.ArgumentException;
import net.oneandone.sushi.fs.DirectoryNotFoundException;
import net.oneandone.sushi.fs.ExistsException;
import net.oneandone.sushi.fs.LineFormat;
import net.oneandone.sushi.fs.LineReader;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.util.Separator;
import net.oneandone.sushi.util.Strings;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

/** Maps absolute directories to svn urls */
public class Fstab {
    public static Fstab load(World world) throws IOException {
        return load(world, ".pommes.fstab");
    }

    private static Fstab load(World world, String name) throws IOException {
        FileNode origin;
        Fstab result;
        String line;

        origin = (FileNode) world.getHome().join(name);
        result = new Fstab();
        if (origin.exists()) {
            try (Reader reader = origin.createReader();
                LineReader src = new LineReader(reader, new LineFormat(LineFormat.LF_SEPARATOR))) {
                while (true) {
                    line = src.next();
                    if (line == null) {
                        break;
                    }
                    result.add(Line.parse(world, line));
                }
            }
        }
        return result;
    }

    public static class Line {
        public static Line parse(World world, String str) throws ExistsException, DirectoryNotFoundException {
            List<String> parts;
            String uri;
            FileNode directory;

            parts = Separator.SPACE.split(str);
            if (parts.size() < 2) {
                throw new IllegalArgumentException(str);
            }
            uri = parts.remove(0);
            uri = withSlash(uri);
            directory = world.file(parts.remove(0));
            directory.checkDirectory();
            return new Line(uri, directory, parts);
        }

        public final String uri;
        public final FileNode directory;
        public final List<String> defaults;

        public Line(String uri, FileNode directory, List<String> defaults) {
            this.uri = uri;
            this.directory = directory;
            this.defaults = defaults;
        }
    }

    private static String withSlash(String url) {
        if (!url.endsWith("/")) {
            url = url + "/";
        }
        return url;
    }

    //--

    private final List<Line> lines;

    public Fstab() {
        this.lines = new ArrayList<>();
    }

    public void add(Line line) {
        lines.add(line);
    }

    public Line lookup(String prefix) {
        for (Line line : lines) {
            if (prefix.equals(line)) {
                return line;
            }
        }
        return null;
    }

    public FileNode locateOpt(String url) {
        for (Line line : lines) {
            if (url.startsWith(line.uri)) {
                return line.directory.join(fold(url.substring(line.uri.length())));
            }
        }
        return null;
    }

    private static final String TRUNK = "/trunk/";
    private static final String BRANCHES = "/branches/";

    public static String fold(String path) {
        int beforeBranches;
        int beforeProjectName;
        // with tailing slash:
        String projectName;
        // with tailing slash:
        String branchName;

        if (!path.endsWith("/")) {
            throw new IllegalArgumentException(path);
        }
        beforeBranches = path.indexOf(BRANCHES);
        if (beforeBranches == -1) {
            if (path.endsWith(TRUNK)) {
                path = path.substring(0, path.length() - TRUNK.length() + 1);
            }
        } else {
            branchName = path.substring(beforeBranches + BRANCHES.length());
            beforeProjectName = path.lastIndexOf('/', beforeBranches - 1);
            if (beforeProjectName == -1) {
                throw new IllegalStateException(path);
            }
            projectName = path.substring(beforeProjectName + 1, beforeBranches + 1);
            path = path.substring(0, beforeProjectName + 1) +
                    ((common(projectName, branchName) >= 3) ? branchName : Strings.removeRight(projectName, "/") + "-" + branchName);
        }
        if (!path.endsWith("/")) {
            throw new IllegalStateException(path);
        }
        return path;
    }

    private static int common(String left, String right) {
        int max;

        max = Math.min(left.length(), right.length());
        for (int i = 0; i < max; i++) {
            if (left.charAt(i) != right.charAt(i)) {
                return i;
            }
        }
        return max;
    }
}
