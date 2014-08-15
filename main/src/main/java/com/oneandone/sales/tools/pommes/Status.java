package com.oneandone.sales.tools.pommes;

import com.oneandone.sales.tools.maven.Maven;
import com.oneandone.sales.tools.pommes.lucene.Database;
import net.oneandone.sushi.cli.ArgumentException;
import net.oneandone.sushi.cli.Console;
import net.oneandone.sushi.cli.Remaining;
import net.oneandone.sushi.fs.DirectoryNotFoundException;
import net.oneandone.sushi.fs.ListException;
import net.oneandone.sushi.fs.file.FileNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Status extends Base {
    private String baseDirectoryString;

    @Remaining
    public void remaining(String str) {
        if (baseDirectoryString == null) {
            baseDirectoryString = str;
        } else {
            throw new ArgumentException("too many arguments");
        }
    }

    public Status(Console console, Maven maven) {
        super(console, maven);
    }

    @Override
    public void invoke() throws Exception {
        FileNode baseDirectory;
        FileMap checkouts;
        List<FileNode> directories;

        baseDirectory = baseDirectoryString == null ? (FileNode) console.world.getWorking() : console.world.file(baseDirectoryString);
        checkouts = FileMap.loadCheckouts(console.world);
        directories = checkouts.under(baseDirectory);
        for (FileNode directory : directories) {
            if (!directory.exists()) {
                console.info.println("! " + directory);
            } else if (Umount.modified(directory)) {
                console.info.println("M " + directory);
            } else if (!checkouts.lookupUrl(directory).equals(scanUrl(directory))) {
                console.info.println("C " + directory + " " + scanUrl(directory) + " vs " + checkouts.lookupUrl(directory));
            } else {
                console.verbose.println("  " + directory);
            }
        }
        for (FileNode node : unknown(baseDirectory, directories)) {
            console.info.println("? " + node);
        }
    }

    private static List<FileNode> unknown(FileNode root, List<FileNode> directories) throws ListException, DirectoryNotFoundException {
        List<FileNode> lst;
        List<FileNode> result;

        result = new ArrayList<>();
        lst = new ArrayList<>();
        for (FileNode directory: directories) {
            candicates(root, directory, lst);
        }
        for (FileNode directory : lst) {
            for (FileNode node : directory.list()) {
                if (node.isFile() || !hasAnchestor(directories, node)) {
                    result.add(node);
                }
            }
        }
        return result;
    }

    private static boolean hasAnchestor(List<FileNode> directories, FileNode node) {
        for (FileNode directory : directories) {
            if (directory.hasAnchestor(node)) {
                return true;
            }
        }
        return false;
    }

    private static void candicates(FileNode root, FileNode directory, List<FileNode> result) {
        FileNode parent;

        if (directory.hasDifferentAnchestor(root)) {
            parent = directory.getParent();
            if (!result.contains(parent)) {
                result.add(parent);
                candicates(root, parent, result);
            }
        }
    }

    private static String scanUrl(FileNode directory) throws IOException {
        String url;
        int idx;

        url = directory.launcher("svn", "info").exec();
        idx = url.indexOf("URL: ") + 5;
        return Database.withSlash(url.substring(idx, url.indexOf("\n", idx)));
    }
}
