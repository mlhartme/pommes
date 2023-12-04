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
package net.oneandone.pommes.storage;

import net.oneandone.inline.ArgumentException;
import net.oneandone.pommes.cli.Environment;
import net.oneandone.pommes.cli.Find;
import net.oneandone.pommes.descriptor.Descriptor;
import net.oneandone.pommes.descriptor.RawDescriptor;
import net.oneandone.pommes.scm.Scm;
import net.oneandone.pommes.scm.ScmUrl;
import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.NodeInstantiationException;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.fs.filter.Filter;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

/** To search files system and subversion */
public class FileStorage extends TreeStorage<FileStorage.Repo, FileNode> {
    public record Repo(FileNode directory, Scm scm) {
    }

    public static Descriptor probe(Environment environment, String storageName, FileNode checkout) throws IOException {
        FileStorage storage;
        List<Repo> dest;

        storage = new FileStorage(environment, storageName, checkout);
        dest = new ArrayList<>(2);
        storage.scan(checkout, false, dest);
        return switch (dest.size()) {
            case 0 -> null;
            case 1 -> storage.load(dest.get(0));
            default -> throw new IOException("ambiguous: " + dest);
        };
    }

    public static FileStorage create(Environment environment, String name, String url) throws URISyntaxException, NodeInstantiationException {
        Node<?> node = Find.fileOrNode(environment.world(), url);
        if (node instanceof FileNode file) {
            return new FileStorage(environment, name, file);
        } else {
            throw new URISyntaxException(url, "file expected");
        }
    }

    //--

    private final FileNode root;
    private final Filter exclude;

    public FileStorage(Environment environment, String name, FileNode root) {
        super(environment, name);
        this.root = root;
        this.exclude = new Filter();
    }

    @Override
    public void addExclude(String str) {
        if (str.startsWith("/") || str.endsWith("/")) {
            throw new ArgumentException("do not use '/' before or after excludes: " + str);
        }
        exclude.include("**/" + str);
    }

    @Override
    public List<Repo> list() throws IOException {
        List<Repo> result = new ArrayList<>();
        scan(root, true, result);
        return result;
    }

    public void scan(FileNode directory, boolean recurse, List<Repo> dest) throws IOException {
        if (!directory.isDirectory()) {
            return;
        }
        if (exclude.matches(directory.getPath())) {
            return;
        }
        Scm scm = Scm.probeCheckout(directory);
        if (scm != null) {
            dest.add(new Repo(directory, scm));
            return;
        }
        if (recurse) {
            // recurse
            for (FileNode child : directory.list()) {
                scan(child, true, dest);
            }
        }
    }


    @Override
    public List<FileNode> listRoot(Repo entry) throws IOException {
        return entry.directory().list();
    }

    @Override
    public String fileName(FileNode file) {
        return file.getName();
    }

    @Override
    public ScmUrl storageUrl(Repo repository) throws IOException {
        return repository.scm().getUrl(repository.directory());
    }

    @Override
    public String repositoryPath(Repo repository, FileNode file) throws IOException {
        return file.getPath();
    }

    @Override
    public Node<?> localFile(Repo repository, FileNode file) throws IOException {
        return file;
    }

    @Override
    public String fileRevision(Repo repository, FileNode fileNode, Node<?> local) throws IOException {
        return Long.toString(fileNode.getLastModified());
    }

    @Override
    public Descriptor createDefault(Repo entry) throws IOException {
        return RawDescriptor.createOpt(name, entry.directory());
    }
}
