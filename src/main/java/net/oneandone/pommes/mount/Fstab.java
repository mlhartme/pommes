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
package net.oneandone.pommes.mount;

import net.oneandone.pommes.model.Pom;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.io.LineFormat;
import net.oneandone.sushi.io.LineReader;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

/** List of mount points */
public class Fstab {
    public static FileNode file(World world) throws IOException {
        return (FileNode) world.getHome().join(".pommes.fstab");
    }

    public static Fstab load(World world) throws IOException {
        return load(file(world));
    }

    public static Fstab load(FileNode file) throws IOException {
        Fstab result;
        String line;

        result = new Fstab();
        if (file.exists()) {
            try (Reader reader = file.newReader();
                LineReader src = new LineReader(reader, new LineFormat(LineFormat.LF_SEPARATOR))) {
                while (true) {
                    line = src.next();
                    if (line == null) {
                        break;
                    }
                    result.add(Point.parse(file.getWorld(), line));
                }
            }
        }
        return result;
    }

    public static String withSlash(String url) {
        if (!url.endsWith("/")) {
            url = url + "/";
        }
        return url;
    }

    //--

    private final List<Point> points;

    public Fstab() {
        this.points = new ArrayList<>();
    }

    public void add(Point point) throws IOException {
        for (Point existing : points) {
            point.checkConflict(existing);
        }
        points.add(point);
    }

    public Point pointOpt(FileNode directory) {
        for (Point point : points) {
            if (point.group(directory) != null) {
                return point;
            }
        }
        return null;
    }

    public List<FileNode> directories(Pom pom) {
        FileNode directory;
        List<FileNode> result;

        result = new ArrayList<>();
        for (Point point : points) {
            directory = point.directoryOpt(pom);
            if (directory != null) {
                result.add(directory);
            }
        }
        return result;
    }

    public void save(FileNode dest) throws IOException {
        StringBuilder buffer;

        buffer = new StringBuilder();
        for (Point point : points) {
            buffer.append(point.toLine());
            buffer.append('\n');
        }
        dest.writeString(buffer.toString());
    }
}
