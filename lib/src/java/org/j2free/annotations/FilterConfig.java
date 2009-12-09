/*
 * FilterConfig.java
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
package org.j2free.annotations;

import java.lang.annotation.*;

/**
 *
 * @author Ryan Wilson
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface FilterConfig {

    public String mapping();

    public boolean requireController() default false;

    // @TODO implement regex filter maappings (this is going to suck...)
    //public String regex() default "";

}
