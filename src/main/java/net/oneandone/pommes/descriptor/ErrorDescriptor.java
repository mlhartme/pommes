/*
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
package net.oneandone.pommes.descriptor;

import net.oneandone.pommes.database.Project;
import net.oneandone.pommes.scm.ScmUrl;

import java.io.IOException;

public class ErrorDescriptor extends Descriptor {
    public static final Descriptor END_OF_QUEUE = new ErrorDescriptor(new IllegalStateException(),
            "end-of-queue", "path", "revision", null);

    private final Exception exception;

    public ErrorDescriptor(Exception exception, String storage, String path, String revision, ScmUrl storageScm) {
        super(storage, path, revision, storageScm);
        this.exception = exception;
    }

    @Override
    public Project load() throws IOException {
        if (exception instanceof RuntimeException) {
            throw (RuntimeException) exception;
        } else {
            throw (IOException) exception;
        }
    }
}
