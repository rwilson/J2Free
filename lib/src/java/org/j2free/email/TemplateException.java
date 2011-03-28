/*
 * TemplateException.java
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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Ryan Wilson
 */
public class TemplateException extends Exception
{
    private Set<String> tokens;

    public TemplateException(String message)
    {
        super(message);
        this.tokens = new HashSet<String>();
    }

    public void addToken(String token)
    {
        this.tokens.add(token);
    }

    /**
     * @return an unmodifiable list of tokens that were not replaced.
     */
    public Set<String> getUnreplacedTokens()
    {
        return Collections.unmodifiableSet(tokens);
    }
}
