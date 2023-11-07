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
package net.oneandone.pommes.descriptor;

import net.oneandone.pommes.cli.Environment;
import net.oneandone.pommes.database.Gav;
import net.oneandone.pommes.database.Project;
import net.oneandone.pommes.scm.Scm;
import net.oneandone.pommes.scm.ScmUrl;
import net.oneandone.sushi.fs.file.FileNode;

import java.io.IOException;

/** descriptor without meta information like form poms, only the scm url is known */
public class RawDescriptor extends Descriptor {
    public static RawDescriptor createOpt(String repository, FileNode node) throws IOException {
        Scm scm;

        if (!node.isDirectory()) {
            return null;
        }
        scm = Scm.probeCheckout(node);
        if (scm == null) {
            return null;
        }
        return new RawDescriptor(scm, node, repository, node.getPath(), Long.toString(node.getLastModified()), scm.getUrl(node));
    }

    private final Scm scm;
    private final FileNode directory;

    public RawDescriptor(Scm scm, FileNode directory, String repository, String path, String revision, ScmUrl repositoryScm) {
        super(repository, path, revision, repositoryScm);
        this.scm = scm;
        this.directory = directory;
    }

    //--

    @Override
    public Project load(Environment environment) throws IOException {
        Gav artifact;

        if (repositoryScm == null || !repositoryScm.same(scm.getUrl(directory))) {
            throw new IllegalArgumentException(repositoryScm + " vs " + scm.getUrl(directory));
        }
        artifact = repositoryScm.defaultGav();
        return new Project(repository, path, revision, null, artifact, repositoryScm, null);
    }
}
