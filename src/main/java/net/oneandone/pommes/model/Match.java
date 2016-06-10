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

public enum Match {
    SUBSTRING, PREFIX, SUFFIX, STRING;

    public static Object[] locate(String str) {
        for (int i = 0; i < str.length(); i++) {
            switch (str.charAt(i)) {
                case ':':
                    return new Object[] { SUBSTRING, i };
                case '=':
                    return new Object[] { STRING, i };
                case '^':
                    return new Object[] { PREFIX, i };
                case '%':
                    return new Object[] { SUFFIX, i };
                default:
                    break;
            }
        }
        return null;
    }
}
