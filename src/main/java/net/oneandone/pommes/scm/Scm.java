package net.oneandone.pommes.scm;

import net.oneandone.sushi.fs.ExistsException;
import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.file.FileNode;

import java.io.IOException;
import java.util.List;

public class Scm {
    public static void scanCheckouts(FileNode directory, List<FileNode> result) throws IOException {
        List<FileNode> children;

        if (isCheckout(directory)) {
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

    public static boolean isCheckout(Node dir) throws ExistsException {
        if (Subversion.isCheckout(dir)) {
            return true;
        }
        return false;
    }
}
