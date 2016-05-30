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
package net.oneandone.pommes.model;

import java.util.ArrayList;
import java.util.List;

public class Pom {
    /** currently always starts with svn:https:// */
    public final String origin;
    public final String revision;

    /** may be null */
    public final Gav parent;
    public final Gav coordinates;
    /** may be null */
    public final String scm;
    public final List<Gav> dependencies;

    public Pom(String origin, String revision, Gav parent, Gav coordinates, String scm) {
        if (origin == null) {
            throw new IllegalArgumentException(origin);
        }
        this.origin = origin;
        this.revision = revision;
        this.parent = parent;
        this.coordinates = coordinates;
        this.scm = scm;
        this.dependencies = new ArrayList<>();
    }

    public String toLine() {
        return coordinates.toGavString() + " @ " + scm;
    }

    @Override
    public String toString() {
        return toLine();
    }
}
