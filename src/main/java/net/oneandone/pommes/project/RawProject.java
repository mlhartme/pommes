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
package net.oneandone.pommes.project;

import net.oneandone.pommes.cli.Environment;
import net.oneandone.pommes.database.Gav;
import net.oneandone.pommes.database.Pom;
import net.oneandone.pommes.scm.Scm;
import net.oneandone.sushi.fs.file.FileNode;

import java.io.IOException;
import java.net.URISyntaxException;

/** project without meta information like form poms, only the scm url is know */
public class RawProject extends Project {
    public static RawProject createOpt(FileNode node) throws IOException {
        Scm scm;

        if (!node.isDirectory()) {
            return null;
        }
        scm = Scm.probeCheckout(node);
        if (scm == null) {
            return null;
        }
        return new RawProject(scm, node);
    }

    private final Scm scm;
    private final FileNode directory;

    public RawProject(Scm scm, FileNode directory) {
        this.scm = scm;
        this.directory = directory;
    }

    //--

    @Override
    protected Pom doLoad(Environment environment, String zone, String origin, String revision, String foundScm) throws IOException {
        Gav artifact;

        if (!foundScm.equals(scm.getUrl(directory))) {
            throw new IllegalArgumentException(foundScm + " " + scm.getUrl(directory));
        }
        try {
            artifact = scm.defaultGav(foundScm);
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
        return new Pom(zone, origin, revision, null, artifact, foundScm, null);
    }
}
