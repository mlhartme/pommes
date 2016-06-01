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
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.util.Strings;

/** mount point in fstab */
public class Point {
    public final FileNode directory;

    public Point(FileNode directory) {
        this.directory = directory;
    }

    public String group(FileNode childDirectory) {
        if (childDirectory.equals(directory)) {
            return "";
        } else if (!childDirectory.hasAnchestor(directory)) {
            return null;
        } else {
            return childDirectory.getRelative(directory).replace('/', '.');
        }
    }

    public FileNode directory(Pom pom) {
        String ga;
        String path;

        ga = pom.coordinates.groupId + "." + pom.coordinates.artifactId;
        path = ga.replace('.', '/');
        return directory.join(withBranch(path, pom));
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
}
