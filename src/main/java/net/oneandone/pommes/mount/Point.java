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
import net.oneandone.sushi.fs.DirectoryNotFoundException;
import net.oneandone.sushi.fs.ExistsException;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.util.Separator;
import net.oneandone.sushi.util.Strings;

import java.io.IOException;
import java.util.List;

/** mount point in fstab */
public class Point {
    public static Point parse(World world, String line) throws ExistsException, DirectoryNotFoundException {
        List<String> parts;
        String group;
        FileNode directory;

        parts = Separator.SPACE.split(line);
        switch (parts.size()) {
            case 1:
                group = "";
                break;
            case 2:
                group = parts.remove(0);
                break;
            default:
                throw new IllegalArgumentException(line);
        }
        directory = world.file(parts.remove(0));
        directory.checkDirectory();
        return new Point(group, directory, parts);
    }

    public final String groupPrefix;
    public final FileNode directory;
    public final List<String> defaults; // TODO: unused

    public Point(String groupPrefix, FileNode directory, List<String> defaults) {
        this.groupPrefix = groupPrefix;
        this.directory = directory;
        this.defaults = defaults;
    }

    public String group(FileNode childDirectory) {
        if (childDirectory.equals(directory)) {
            // to avoid "." returned by get relative
            return groupPrefix;
        } else if (!childDirectory.hasAnchestor(directory)) {
            return null;
        } else {
            return groupPrefix + "." + childDirectory.getRelative(directory).replace('/', '.');
        }
    }

    public FileNode directory(Pom pom) {
        FileNode result;

        result = directoryOpt(pom);
        if (result == null) {
            throw new IllegalStateException(pom.toLine());
        }
        return result;
    }

    public FileNode directoryOpt(Pom pom) {
        String ga;
        String remaining;

        ga = pom.coordinates.groupId + "." + pom.coordinates.artifactId;
        if (ga.startsWith(groupPrefix)) {
            remaining = ga.substring(groupPrefix.length());
            if (!remaining.isEmpty()) {
                if (groupPrefix.isEmpty()) {
                    // nothing to do
                } else {
                    if (remaining.charAt(0) != '.') {
                        return null;
                    }
                    remaining = remaining.substring(1);
                }
                remaining = remaining.replace('.', '/');
            }
            return directory.join(withBranch(remaining, pom));
        } else {
            return null;
        }
    }

    public void checkConflict(Point existing) throws IOException {
        if (directory.hasAnchestor(existing.directory) || existing.directory.hasAnchestor(directory)) {
            throw new IOException("conflicting mount points: " + directory + " vs " + existing.directory);
        }
    }

    //--


    private static final String BRANCHES = "/branches/";

    public static String withBranch(String base, Pom pom) {
        String scm;
        int idx;
        String branchName;
        String projectName;

        if (base.endsWith("/")) {
            throw new IllegalArgumentException(base);
        }
        scm = pom.scm;
        if (scm == null) {
            return base;
        }
        idx = scm.indexOf(BRANCHES);
        if (idx == -1) {
            return base;
        }
        branchName = Strings.removeRightOpt(scm.substring(idx + BRANCHES.length()), "/");
        projectName = base.substring(base.lastIndexOf('/') + 1); // ok if not found
        if (common(projectName, branchName) > 2) {
            return base.substring(0, base.length() - projectName.length()) + branchName;
        } else {
            return base + "-" + branchName;
        }
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

    public String toLine() {
        StringBuilder result;

        result = new StringBuilder();
        result.append(groupPrefix);
        result.append(' ');
        result.append(directory.getAbsolute());
        for (String dflt : defaults) {
            result.append(' ');
            result.append(dflt);
        }
        return result.toString();
    }
}
