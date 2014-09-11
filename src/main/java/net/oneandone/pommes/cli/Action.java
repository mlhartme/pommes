/**
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
package net.oneandone.pommes.cli;

import net.oneandone.sushi.cli.Console;
import net.oneandone.sushi.fs.DeleteException;
import net.oneandone.sushi.fs.MkdirException;
import net.oneandone.sushi.fs.NodeNotFoundException;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.launcher.Failure;
import net.oneandone.sushi.launcher.Launcher;

import java.io.IOException;

public abstract class Action implements Comparable<Action> {
    protected final FileNode directory;
    protected final String svnurl;

    protected Action(FileNode directory, String svnurl) {
        this.directory = directory;
        this.svnurl = svnurl;
    }

    public abstract String status();
    public abstract void run(Console console) throws Exception;

    @Override
    public int compareTo(Action action) {
        return directory.getPath().compareTo(action.directory.getPath());
    }

    //--

    public static class StatusException extends IOException {
        public StatusException(String status) {
            super(status);
        }
    }

    public static class Noop extends Action {
        public Noop(FileNode directory, String svnurl) {
            super(directory, svnurl);
        }

        @Override
        public String status() {
            return "  " + directory + " (" + svnurl + ")";
        }

        @Override
        public void run(Console console) throws Exception {
        }
    }

    public static class Checkout extends Action {
        public static Checkout createOpt(FileNode directory, String svnurl) throws StatusException {
            String scannedUrl;

            if (directory.exists()) {
                try {
                    scannedUrl = Base.scanUrl(directory);
                } catch (IOException e) {
                    scannedUrl = "[scan failed: " + e.getMessage() + "]";
                }
                if (svnurl.equals(scannedUrl)) {
                    return null;
                } else {
                    throw new StatusException("C " + directory + " (" + svnurl + " vs " + scannedUrl + ")");
                }
            } else {
                return new Checkout(directory, svnurl);
            }
        }

        public Checkout(FileNode directory, String svnurl) {
            super(directory, svnurl);
        }

        public String status() {
            return "A " + directory + " (" + svnurl + ")";
        }

        public void run(Console console) throws MkdirException, Failure {
            Launcher svn;

            directory.getParent().mkdirsOpt();
            svn = Base.svn(directory.getParent(), "co", svnurl, directory.getName());
            if (console.getVerbose()) {
                console.verbose.println(svn.toString());
            } else {
                console.info.println("svn co " + svnurl + " /" + directory.getAbsolute());
            }
            if (console.getVerbose()) {
                svn.exec(console.verbose);
            } else {
                // exec into string (and ignore it) - otherwise, Failure Exceptions cannot contains the output
                svn.exec();
            }
        }
    }

    public static class Remove extends Action {
        public static Remove create(FileNode directory, String svnurl) throws IOException {
            if (Base.notCommitted(directory)) {
                throw new StatusException("M " + directory + " (" + svnurl + ")");
            }
            return new Remove(directory, svnurl);
        }

        public Remove(FileNode directory, String svnurl) {
            super(directory, svnurl);
        }

        public String status() {
            return "D " + directory + " (" + svnurl + ")";
        }

        public void run(Console console) throws NodeNotFoundException, DeleteException {
            console.info.println("rm -rf " + directory);
            directory.deleteTree();
        }
    }
}
