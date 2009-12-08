/*
 * FilterMapping.java
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
package org.j2free.invoker;

import javax.servlet.Filter;
import org.j2free.util.Constants;

/**
 *
 * @author Ryan Wilson
 */
final class FilterMapping implements Comparable<FilterMapping> {

    protected final Filter filter;
    protected final String path;
    
    private final int depth;

    protected FilterMapping(Filter filter, String path) {
        this.filter = filter;
        this.path   = path.replace("*", Constants.EMPTY); // trim the "*" off the end
        this.depth  = this.path.split("/").length;
    }

    /**
     * Orders FilterMappings their "depth"
     * @param o
     * @return
     */
    public int compareTo(FilterMapping o) {
        if (this.depth < o.depth) {
            return -1;
        } else if (this.depth > o.depth) {
            return 1;
        } else {
            return 0;
        }
    }

    /**
     * @param path A URI
     * @return <tt>true</tt> if this filter should be run for
     *         the given path, otherwise <tt>false</tt>
     */
    public boolean appliesTo(String uri) {
        return uri.startsWith(this.path);
    }
}
