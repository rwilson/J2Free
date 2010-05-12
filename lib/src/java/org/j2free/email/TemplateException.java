/*
 * TemplateException.java
 *
 * Copyright (c) 2009 FooBrew, Inc.
 */
package org.j2free.email;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
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
    public List<String> getUnreplacedTokens()
    {
        return Collections.unmodifiableList(Arrays.asList(tokens.toArray(new String[tokens.size()])));
    }
}