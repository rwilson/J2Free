/*
 * J2FreeServlet.java
 *
 * Copyright (c) 2009 FooBrew, Inc.
 */
package org.j2free.servlet;

import java.util.concurrent.atomic.AtomicReference;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import org.j2free.util.UnaryFunctor;

/**
 * Provides an extension class for easy access to the User.
 *
 * @author Ryan Wilson
 */
public class J2FreeServlet extends HttpServlet {

    private static final UnaryFunctor EMPTY_FUNCTOR = new UnaryFunctor() {
        public Object run(Object o) {
            return null;
        }
    };

    private static AtomicReference<UnaryFunctor> getUserFunctor = new AtomicReference<UnaryFunctor>(EMPTY_FUNCTOR);

    public static void defineGetUser(UnaryFunctor<HttpServletRequest> functor) {
        getUserFunctor.set(functor);
    }

    protected <T> T getUser(HttpServletRequest request) {
        return (T)getUserFunctor.get().run(request);
    }

}
