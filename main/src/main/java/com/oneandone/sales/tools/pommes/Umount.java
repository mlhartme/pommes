package com.oneandone.sales.tools.pommes;

import com.oneandone.sales.tools.maven.Maven;
import net.oneandone.sushi.cli.ArgumentException;
import net.oneandone.sushi.cli.Console;
import net.oneandone.sushi.cli.Remaining;
import net.oneandone.sushi.fs.file.FileNode;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class Umount extends Base {
    private String baseDirectoryString;

    @Remaining
    public void remaining(String str) {
        if (baseDirectoryString == null) {
            baseDirectoryString = str;
        } else {
            throw new ArgumentException("too many arguments");
        }
    }

    public Umount(Console console, Maven maven) {
        super(console, maven);
    }

    @Override
    public void invoke() throws Exception {
        FileNode baseDirectory;
        FileMap mounts;
        FileMap checkouts;
        Map<FileNode, String> mountRemoves;
        Map<FileNode, String> checkoutRemoves;
        String url;

        baseDirectory = baseDirectoryString == null ? (FileNode) console.world.getWorking() : console.world.file(baseDirectoryString);
        mounts = FileMap.loadMounts(console.world);
        checkouts = FileMap.loadCheckouts(console.world);
        mountRemoves = new LinkedHashMap<>();
        checkoutRemoves = new LinkedHashMap<>();
        for (FileNode mount : mounts.under(baseDirectory)) {
            url = mounts.lookupUrl(mount);
            mountRemoves.put(mount, url);
            for (FileNode checkout : checkouts.under(mount)) {
                checkoutRemoves.put(checkout, checkouts.lookupUrl(checkout));
            }
        }
        if (mountRemoves.size() == 0) {
            throw new ArgumentException("nothing to unmount under " + baseDirectory);
        } else {
            performUpdate(mounts, Collections.<FileNode, String>emptyMap(), mountRemoves,
                    checkouts, Collections.<FileNode, String>emptyMap(), checkoutRemoves);
        }
    }
}
