/**
 * Template.java
 *
 * Copyright 2011 FooBrew, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.j2free.email;

import net.jcip.annotations.Immutable;

/**
 * A wrapper for holding a template and it's corresponding
 * content-type.
 *
 * @author Ryan Wilson
 */
@Immutable
public class Template
{
    public final String templateText;
    public final EmailService.ContentType contentType;

    public Template(String templateText, EmailService.ContentType contentType)
    {
        this.templateText = templateText;
        this.contentType = contentType;
    }
}
