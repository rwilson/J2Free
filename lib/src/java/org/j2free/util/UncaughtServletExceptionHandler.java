/*
 * UncaughtServletExceptionHandler.java
 *
 * Copyright (c) 2009 FooBrew, Inc.
 */
package org.j2free.util;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 * Interface class so a J2Free application can provide
 * a way for InvokerFilter to handle uncaught exceptions
 * that bubble up to it's level.
 *
 * @author Ryan
 */
public interface UncaughtServletExceptionHandler {

    public void handleException(ServletRequest request, ServletResponse response, Throwable e)
            throws ServletException, IOException;

}
