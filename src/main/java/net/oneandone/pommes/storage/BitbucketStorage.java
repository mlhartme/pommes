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

import net.oneandone.pommes.cli.Environment;
import net.oneandone.pommes.descriptor.Descriptor;
import net.oneandone.pommes.scm.GitUrl;
import net.oneandone.pommes.scm.ScmUrl;
import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.NodeInstantiationException;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.http.HttpNode;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

/** https://developer.atlassian.com/static/rest/bitbucket-server/4.6.2/bitbucket-rest.html */
public class BitbucketStorage extends TreeStorage<String, String> {
    public static void main(String[] args) throws IOException {
        World world;
        Bitbucket bb;

        world = World.create();
        bb = new Bitbucket(((HttpNode) world.validNode("https://bitbucket.1and1.org")).getRoot());
        System.out.println("rev: " + new String(bb.readBytes("CISOOPS", "puc", "pom.xml")));
    }

    public static BitbucketStorage create(Environment environment, String name, String url) throws URISyntaxException, NodeInstantiationException {
        return new BitbucketStorage(environment, name, (HttpNode) environment.world().node(url));
    }

    private final Environment environment;
    private final HttpNode bitbucket;
    private final Bitbucket bb;
    private final String bbProject;

    public BitbucketStorage(Environment environment, String name, HttpNode bitbucket) {
        super(environment, name);
        this.environment = environment;
        this.bitbucket = bitbucket;
        this.bb = new Bitbucket(bitbucket.getRoot());
        this.bbProject = bitbucket.getName();
    }

    @Override
    public List<String> list() throws IOException {
        return bb.listRepos(bbProject);
    }

    @Override
    public List<String> listRoot(String repo) throws IOException {
        return bb.listRoot(bbProject, repo);
    }

    @Override
    public String fileName(String file) {
        return file;
    }

    @Override
    public ScmUrl storageUrl(String repository) throws IOException {
        String hostname = bitbucket.getRoot().getHostname();
        return GitUrl.create("ssh://git@" + hostname + "/" + bbProject.toLowerCase() + "/" + repository + ".git");
    }

    @Override
    public String repositoryPath(String repository, String file) throws IOException {
        return bbProject.toLowerCase() + "/" + repository + "/" + file;
    }

    @Override
    public Node<?> localFile(String repository, String file) throws IOException {
        byte[] bytes = bb.readBytes(bbProject, repository, file);
        return environment.world().memoryNode(bytes);
    }

    @Override
    public String fileRevision(String repository, String s, Node<?> local) throws IOException {
        return local.sha();
    }

    @Override
    public Descriptor createDefault(String repo) {
        return null; // TODO
    }
}
