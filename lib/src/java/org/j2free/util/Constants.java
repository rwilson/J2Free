/*
 * Constants.java
 *
 * Created on April 5, 2008, 1:44 AM
 *
 * Copyright (c) 2008 Publi.us
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

package org.j2free.util;

/**
 * @author Ryan Wilson (http://blog.augmentedfragments.com)
 */
public class Constants {
    
    // Development with no network access
    public static final int RUN_MODE_LOCAL       = 0;
    
    // Enables network access
    public static final int RUN_MODE_DEVELOPMENT = 1;
    
    // Production 
    public static final int RUN_MODE_PRODUCTION  = 2;

    public static volatile int RUN_MODE = RUN_MODE_PRODUCTION;
}
