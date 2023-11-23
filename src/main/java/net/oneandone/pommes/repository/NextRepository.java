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
package net.oneandone.pommes.repository;

import net.oneandone.inline.Console;
import net.oneandone.pommes.descriptor.Descriptor;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.BlockingQueue;

public abstract class NextRepository<T> extends Repository {
    public NextRepository(String name) {
        super(name);
    }

    @Override
    public void scan(BlockingQueue<Descriptor> dest, Console console) throws IOException, URISyntaxException, InterruptedException {
        console.info.println("collecting projects ...");
        List<T> projects = doScan();
        console.info.println("collected " + projects.size());
        for (T project : projects) {
            try {
                var descriptor = scanOpt(project);
                if (descriptor != null) {
                    dest.put(descriptor);
                }
            } catch (IOException e) {
                console.error.println("cannot load " + project + ": " + e.getMessage());
                e.printStackTrace(console.verbose);
            }
        }
    }

    public abstract List<T> doScan() throws IOException;
    public abstract Descriptor scanOpt(T project) throws IOException;
}
