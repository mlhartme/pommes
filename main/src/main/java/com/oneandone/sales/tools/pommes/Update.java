package com.oneandone.sales.tools.pommes;

import com.oneandone.sales.tools.maven.Maven;
import net.oneandone.sushi.cli.ArgumentException;
import net.oneandone.sushi.cli.Console;
import net.oneandone.sushi.cli.Remaining;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.launcher.Launcher;

import java.util.List;

public class Update extends Base {
    private String baseDirectoryString;

    @Remaining
    public void remaining(String str) {
        if (baseDirectoryString == null) {
            baseDirectoryString = str;
        } else {
            throw new ArgumentException("too many arguments");
        }
    }

    public Update(Console console, Maven maven) {
        super(console, maven);
    }

    @Override
    public void invoke() throws Exception {
        FileNode baseDirectory;
        FileMap checkouts;
        List<FileNode> directories;
        Launcher svn;

        baseDirectory = baseDirectoryString == null ? (FileNode) console.world.getWorking() : console.world.file(baseDirectoryString);
        checkouts = FileMap.loadCheckouts(console.world);
        directories = checkouts.under(baseDirectory);
        for (FileNode directory : directories) {
            svn = svn(directory, "up");
            if (console.getVerbose()) {
                console.verbose.println(svn.toString());
            } else {
                console.info.println("[svn up " + directory + "]");
            }
            svn.exec(console.info);
        }
    }
}
