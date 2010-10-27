package org.j2free.invoker;

import javax.servlet.http.HttpServletRequest;

/**
 *
 * @author Ryan Wilson
 */
public interface RequestExaminer
{
    public boolean isSSL(HttpServletRequest req);
}
