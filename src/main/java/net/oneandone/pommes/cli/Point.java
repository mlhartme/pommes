package net.oneandone.pommes.cli;

import net.oneandone.sushi.fs.DirectoryNotFoundException;
import net.oneandone.sushi.fs.ExistsException;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.util.Separator;

import java.util.List;

/**
* Created by mhm on 11.09.14.
*/
public class Point {
    public static Point parse(World world, String str) throws ExistsException, DirectoryNotFoundException {
        List<String> parts;
        String uri;
        FileNode directory;

        parts = Separator.SPACE.split(str);
        if (parts.size() < 2) {
            throw new IllegalArgumentException(str);
        }
        uri = parts.remove(0);
        uri = Fstab.withSlash(uri);
        directory = world.file(parts.remove(0));
        directory.checkDirectory();
        return new Point(uri, directory, parts);
    }

    public final String uri;
    public final FileNode directory;
    public final List<String> defaults;

    public Point(String uri, FileNode directory, List<String> defaults) {
        if (!uri.endsWith("/")) {
            throw new IllegalArgumentException(uri);
        }
        this.uri = uri;
        this.directory = directory;
        this.defaults = defaults;
    }

    public String svnurl(FileNode child) {
        return child.hasAnchestor(directory) ? uri + "/" + child.getRelative(directory) : null;
    }

    public FileNode directory(String svnurl) {
        FileNode result;

        result = directoryOpt(svnurl);
        if (result == null) {
            throw new IllegalStateException(svnurl);
        }
        return result;
    }

    public FileNode directoryOpt(String svnurl) {
        if (svnurl.startsWith(uri)) {
            return directory.join(Fstab.fold(svnurl.substring(uri.length())));
        } else {
            return null;
        }
    }
}
