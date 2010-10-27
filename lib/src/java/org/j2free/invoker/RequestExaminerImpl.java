package org.j2free.invoker;

import javax.servlet.http.HttpServletRequest;

/**
 * Default implementation of determining if a request is secure.
 *
 * @author Ryan Wilson
 */
class RequestExaminerImpl implements RequestExaminer
{
    public boolean isSSL(HttpServletRequest req)
    {
        return req.isSecure();
    }
}
