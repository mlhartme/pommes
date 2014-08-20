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

import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.StringUtils;

/**
 * Project coordinates: groupId:artifactId:version
 */
public class GroupArtifactVersion {

    private String groupId;
    private String artifactId;
    private String version;

    public GroupArtifactVersion(String groupId, String artifactId, String version) {
        if (StringUtils.isEmpty(groupId)) {
            throw new IllegalArgumentException("null or empty groupId not allowed");
        }
        this.groupId = StringUtils.trim(groupId);

        if (StringUtils.isEmpty(artifactId)) {
            throw new IllegalArgumentException("null or empty artifactId not allowed");
        }
        this.artifactId = StringUtils.trim(artifactId);

        this.version = StringUtils.trim(version);
        if (StringUtils.isEmpty(this.version)) {
            this.version = "0";
        }
    }

    /**
     * Instantiates a new group artifact version.
     * @param gav
     *            the coordantes in format groupId:artifactId:version
     */
    public GroupArtifactVersion(String gav) {
        if (gav == null) {
            throw new IllegalArgumentException("Null argument");
        }

        String[] splitted = gav.split(":", 3);
        if (splitted.length < 2) {
            throw new IllegalArgumentException("groupId and artifactId reqiried");
        }

        String theVersion = splitted.length > 2 ? splitted[2] : null;
        if (StringUtils.isEmpty(splitted[0])) {
            throw new IllegalArgumentException("null or empty groupId not allowed");
        }
        this.groupId = StringUtils.trim(splitted[0]);

        if (StringUtils.isEmpty(splitted[1])) {
            throw new IllegalArgumentException("null or empty artifactId not allowed");
        }
        this.artifactId = StringUtils.trim(splitted[1]);

        this.version = StringUtils.trim(theVersion);
        if (StringUtils.isEmpty(this.version)) {
            this.version = "0";
        }
    }

    /**
     * Instantiates a new group artifact version by a maven project.
     * @param mavenProject
     *            the maven project
     */
    public GroupArtifactVersion(MavenProject mavenProject) {
        String theGroupId = mavenProject.getGroupId();
        String theArtifactId = mavenProject.getArtifactId();
        if (StringUtils.isEmpty(theGroupId)) {
            throw new IllegalArgumentException("null or empty groupId not allowed");
        }
        this.groupId = StringUtils.trim(theGroupId);

        if (StringUtils.isEmpty(theArtifactId)) {
            throw new IllegalArgumentException("null or empty artifactId not allowed");
        }
        this.artifactId = StringUtils.trim(theArtifactId);

        this.version = StringUtils.trim(mavenProject.getVersion());
        if (StringUtils.isEmpty(this.version)) {
            this.version = "0";
        }
    }

    /**
     * Instantiates a new group artifact version by a dependency.
     * @param dependency
     *            the dependency
     */
    public GroupArtifactVersion(Dependency dependency) {
        String theGroupId = dependency.getGroupId();
        String theArtifactId = dependency.getArtifactId();
        if (StringUtils.isEmpty(theGroupId)) {
            throw new IllegalArgumentException("null or empty groupId not allowed");
        }
        this.groupId = StringUtils.trim(theGroupId);

        if (StringUtils.isEmpty(theArtifactId)) {
            throw new IllegalArgumentException("null or empty artifactId not allowed");
        }
        this.artifactId = StringUtils.trim(theArtifactId);

        this.version = StringUtils.trim(dependency.getVersion());
        if (StringUtils.isEmpty(this.version)) {
            this.version = "0";
        }
    }

    /**
     * Gets the groupId:artifactId string.
     * @return the groupId:artifactId
     */
    public String toGaString() {
        return groupId + ":" + artifactId;
    }

    /**
     * Gets the groupId:artifactId:version string.
     * @return the groupId:artifactId:version string
     */
    public String toGavString() {
        return toGaString() + ":" + version;
    }

    /**
     * Gets the group id.
     * @return the group id
     */
    public String getGroupId() {
        return groupId;
    }

    /**
     * Gets the artifact id.
     * @return the artifact id
     */
    public String getArtifactId() {
        return artifactId;
    }

    /**
     * Gets the version, may be a range.
     * @return the version, may be a range
     */
    public String getVersion() {
        return version;
    }

    @Override
    public String toString() {
        return toGavString();
    }

    public boolean gavEquals(GroupArtifactVersion other) {
        if (artifactId == null) {
            if (other.artifactId != null) {
                return false;
            }
        } else if (!artifactId.equals(other.artifactId)) {
            return false;
        }
        if (groupId == null) {
            if (other.groupId != null) {
                return false;
            }
        } else if (!groupId.equals(other.groupId)) {
            return false;
        }
        if (version == null) {
            if (other.version != null) {
                return false;
            }
        } else if (!version.equals(other.version)) {
            return false;
        }
        return true;
    }

}
