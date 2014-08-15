package com.oneandone.sales.tools.pommes;

import com.oneandone.sales.tools.maven.Maven;
import net.oneandone.sushi.cli.ArgumentException;
import net.oneandone.sushi.cli.Console;
import net.oneandone.sushi.cli.Remaining;
import net.oneandone.sushi.fs.file.FileNode;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class Remount extends Base {
    private String dirString;

    @Remaining
    public void remaining(String str) {
        if (dirString == null) {
            dirString = str;
        } else {
            throw new ArgumentException("too many arguments");
        }
    }

    public Remount(Console console, Maven maven) {
        super(console, maven);
    }

    @Override
    public void invoke() throws Exception {
        FileNode root;
        FileMap mounts;
        FileMap checkouts;
        Map<FileNode, String> adds;
        Map<FileNode, String> removes;

        root = dirString == null ? (FileNode) console.world.getWorking() : console.world.file(dirString);
        mounts = FileMap.loadMounts(console.world);
        checkouts = FileMap.loadCheckouts(console.world);
        adds = new LinkedHashMap<>();
        removes = new LinkedHashMap<>();
        for (FileNode mount : mounts.under(root)) {
            scanUpdate(checkouts, mounts.lookupUrl(mount), mount, adds, removes);
        }
        performUpdate(mounts, Collections.<FileNode, String>emptyMap(), Collections.<FileNode, String>emptyMap(),
                checkouts, adds, removes);
    }
}
