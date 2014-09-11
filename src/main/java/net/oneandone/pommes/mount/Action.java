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
package net.oneandone.pommes.mount;

import net.oneandone.sushi.cli.Console;
import net.oneandone.sushi.fs.file.FileNode;

public abstract class Action implements Comparable<Action> {
    public final FileNode directory;
    public final String svnurl;

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

}
