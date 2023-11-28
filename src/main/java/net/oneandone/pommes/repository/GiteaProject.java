package net.oneandone.pommes.repository;

import io.gitea.ApiException;
import io.gitea.api.RepositoryApi;
import io.gitea.model.ContentsResponse;
import net.oneandone.pommes.cli.Environment;
import net.oneandone.pommes.descriptor.Descriptor;
import net.oneandone.pommes.scm.GitUrl;
import net.oneandone.sushi.fs.memory.MemoryNode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

public record GiteaProject(String org, String repo, String ref) {
    public Descriptor scanOpt(Environment environment, String hostname, RepositoryApi repositoryApi) throws IOException, ApiException {
        List<ContentsResponse> lst;

        lst = repositoryApi.repoGetContentsList(org, repo, ref);
        for (ContentsResponse contents : lst) {
            if ("file".equals(contents.getType())) {
                Descriptor.Creator m = Descriptor.match(contents.getName());
                if (m != null) {
                    contents = repositoryApi.repoGetContents(org, repo, contents.getPath(), ref);
                    String str = contents.getContent();
                    if ("base64".equals(contents.getEncoding())) {
                        str = new String(Base64.getDecoder().decode(str.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
                    } else if (contents.getEncoding() != null) {
                        throw new IllegalStateException(contents.getEncoding());
                    }
                    MemoryNode tmp = environment.world().memoryNode(str);
                    return m.create(environment, tmp, repo, org + "/" + repo + "/" + contents.getPath(), tmp.sha(),
                            GitUrl.create("ssh://gitea@" + hostname + "/" + org + "/" + repo + ".git"));
                }
            }
        }
        return null;
    }
}
