package net.oneandone.pommes.scm;

import net.oneandone.sushi.fs.file.FileNode;

import java.io.IOException;
import java.util.List;

public class Scm {
    public static void scanCheckouts(FileNode directory, List<FileNode> result) throws IOException {
        List<FileNode> children;

        if (directory.join(".svn").isDirectory()) {
            result.add(directory);
        } else {
            children = directory.list();
            if (children != null) {
                for (FileNode child : children) {
                    scanCheckouts(child, result);
                }
            }
        }
    }
}
