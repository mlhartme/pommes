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
package net.oneandone.sales.tools.pommes;

import net.oneandone.sushi.fs.LineFormat;
import net.oneandone.sushi.fs.LineReader;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.file.FileNode;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/** Maps absolute directories to svn urls */
public class FileMap {
    private static final char SEPARATOR = ' ';

    public static FileMap loadMounts(World world) throws IOException {
        return load(world, ".pommes.mounts");
    }

    public static FileMap loadCheckouts(World world) throws IOException {
        return load(world, ".pommes.checkouts");
    }

    private static FileMap load(World world, String name) throws IOException {
        FileMap result;
        String line;
        int idx;

        result = new FileMap((FileNode) world.getHome().join(name));
        if (result.origin.exists()) {
            try (Reader reader = result.origin.createReader();
                 LineReader src = new LineReader(reader, new LineFormat(LineFormat.LF_SEPARATOR))) {
                while (true) {
                    line = src.next();
                    if (line == null) {
                        break;
                    }
                    idx = line.lastIndexOf(' ');
                    if (idx == -1) {
                        throw new IOException("invalid line: " + line);
                    }
                    result.addDirectory(world.file(line.substring(0, idx).trim()), line.substring(idx + 1).trim());
                }
            }
        }
        return result;
    }
    //--

    /** maps File Node to Svn Url Strings (starting with https://). */
    private final SortedMap<FileNode, String> directories;

    private final FileNode origin;

    public FileMap(FileNode origin) {
        this.origin = origin;
        this.directories = new TreeMap<>(new Comparator<FileNode>() {
            @Override
            public int compare(FileNode left, FileNode right) {
                return left.getAbsolute().compareTo(right.getAbsolute());
            }
        });
    }

    //--

    public void addDirectory(FileNode directory, String url) {
        for (FileNode existing : directories.keySet()) {
            if (directory.hasDifferentAnchestor(existing) || existing.hasDifferentAnchestor(directory)) {
                throw new IllegalArgumentException("directories cannot contain each other: " + directory + " vs " + existing);
            }
        }
        if (url.indexOf(SEPARATOR) != -1) {
            throw new IllegalArgumentException(url);
        }
        directories.put(directory, url);
    }

    public void removeDirectory(FileNode directory) {
        if (directories.remove(directory) == null) {
            throw new IllegalArgumentException(directory.toString());
        }
    }

    public String lookupUrl(FileNode directory) {
        return directories.get(directory);
    }

    public List<FileNode> lookupDirectories(String url) {
        List<FileNode> result;

        result = new ArrayList<>();
        for (Map.Entry<FileNode, String> entry : directories.entrySet()) {
            if (url.equals(entry.getValue())) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    public Set<FileNode> directories() {
        return directories.keySet();
    }

    public List<FileNode> under(FileNode parent) {
        List<FileNode> result;
        FileNode file;

        result = new ArrayList<>();
        for (Map.Entry<FileNode, String> entry : directories.entrySet()) {
            file = entry.getKey();
            if (file.hasAnchestor(parent)) {
                result.add(file);
            }
        }
        return result;
    }

    public void save() throws IOException {
        try (Writer writer = origin.createWriter()) {
            for (Map.Entry<FileNode, String> entry : directories.entrySet()) {
                writer.write(entry.getKey().getAbsolute());
                writer.write(SEPARATOR);
                writer.write(entry.getValue());
                writer.write('\n');
            }
        }
    }
}
