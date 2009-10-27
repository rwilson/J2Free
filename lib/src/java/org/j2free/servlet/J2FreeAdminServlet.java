/*
 * J2FreeAdminServlet.java
 *
 * Created on April 3, 2008, 1:07 AM
 *
 * Copyright (c) 2008 Foo Brew, Inc.
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

package org.j2free.servlet;

import java.io.*;
import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.servlet.*;
import javax.servlet.http.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import org.j2free.annotations.ServletConfig;
import org.j2free.admin.Marshaller;
import org.j2free.admin.MarshallingException;
import org.j2free.jpa.Controller;
import org.j2free.util.CharArrayWrapper;
import org.j2free.util.ServletUtils;

/**
 *
 * @author Ryan Wilson
 * @version
 */
@ServletConfig
public class J2FreeAdminServlet extends HttpServlet {
    
    private static Log log = LogFactory.getLog(J2FreeAdminServlet.class);
    
    private static TreeMap<String,Class> entityLookup;
    
    private static final String DISPATCH_ADMIN_JSP       = "/WEB-INF/j2free/jsp/Admin.jsp";
    private static final String DISPATCH_ENTITY_LIST     = "/WEB-INF/j2free/jsp/EntityList.jsp";
    private static final String DISPATCH_ENTITY_SELECTOR = "/WEB-INF/j2free/jsp/EntityBrowser.jsp";
    private static final String DISPATCH_ENTITY_EDIT     = "/WEB-INF/j2free/jsp/EntityEdit.jsp";
    private static final String DISPATCH_ENTITY_INSPECT  = "/WEB-INF/j2free/jsp/EntityInspect.jsp";
    private static final String DISPATCH_ENTITY_CREATE   = "/WEB-INF/j2free/jsp/EntityCreate.jsp";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        
        String url = request.getRequestURL().toString();
        if (!url.endsWith("/")) {
            response.sendRedirect(url + "/");
            return;
        }
        
        if (entityLookup == null) {
            entityLookup = new TreeMap<String,Class>();
            snoopEntities();
        }
        
        request.setAttribute("availableEntities",entityLookup.values());
/*
        if (action.equalsIgnoreCase("create")) {
            action = ACTION_CREATE;
 
            try {
                Class klass = Class.forName(path[2]);
                Marshaller marshaller = Marshaller.getForClass(klass);
                request.setAttribute("fields",marshaller.marshallOut(null,true));
            } catch (Exception e) {
                // Handle error
            }
 
        } else if (action.equalsIgnoreCase("delete")) {
            action = ACTION_DELETE;
 
            try {
                Class klass = Class.forName(path[2]);
                data = pb.findPrimaryKey(klass,Integer.parseInt(path[3]));
            } catch (Exception e) {
                // Handle error
            }
 
        } else if (action.equalsIgnoreCase("edit")) {
            action = ACTION_EDIT;
 
            try {
                Class klass = Class.forName(path[2]);
                data = pb.findPrimaryKey(klass,Integer.parseInt(path[3]));
 
                log.debug(data);
 
                Marshaller marshaller = Marshaller.getForClass(klass);
                request.setAttribute("fields",marshaller.marshallOut(data,true));
                request.setAttribute("object",data);
 
            } catch (Exception e) {
                // Handle error
            }
 
        } else { }
 */
        
        request.getRequestDispatcher(DISPATCH_ADMIN_JSP).forward(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {

        Controller controller = Controller.get(); // Get the controller associated with the current thread;
        
        String uri = request.getRequestURI();
        uri = uri.replaceAll("/j2free/","");
        String path[] = uri.split("/");
        
        log.debug(uri);
        log.debug("path.length = " + path.length);
        
        if (path.length < 1) {
            log.debug("too little path info " + uri);
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        
        RequestDispatcher rd = null;
        
        if (path[0].equals("list")) {
            log.debug("in list");
            if (path.length < 2) {
                log.debug("too few parts for list");
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
            try {
                Class klass = entityLookup.get(path[1]);

                if (klass == null) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    response.getWriter().println("Could not find class for entity type: " + path[1]);
                    return;
                }
                
                Marshaller marshaller = Marshaller.getForClass(klass);
                
                int start = ServletUtils.getIntParameter(request,"start",0);
                int limit = ServletUtils.getIntParameter(request,"limit",100);
                
                Field  entityIdField     = marshaller.getEntityIdField();
                
                if (entityIdField == null) {
                    response.setStatus(HttpServletResponse.SC_NO_CONTENT);
                    return;
                }
                
                String entityIdFieldName = entityIdField.getName();
                
                List entities;
                if (path.length == 3) {
                    String[] stringIds = path[2].split(",");
                    Object[] ignoredIds = new Object[stringIds.length];
                    
                    // NOTE: this will only work with integer entityIds
                    if (entityIdField.getType() == Integer.class) {
                        for (int i = 0; i < stringIds.length; i++)
                            ignoredIds[i] = Integer.parseInt(stringIds[i]);
                    } else if (entityIdField.getType() == String.class) {
                        for (int i = 0; i < stringIds.length; i++)
                            ignoredIds[i] = stringIds[i];
                    } else if (entityIdField.getType() == Long.class) {
                        for (int i = 0; i < stringIds.length; i++)
                            ignoredIds[i] = Long.parseLong(stringIds[i]);
                    }
                    
                    entities = controller.listByCriterions(klass,start,limit,Order.asc(entityIdFieldName),Restrictions.not(Restrictions.in(entityIdFieldName,ignoredIds)));
                } else
                    entities = controller.listByCriterions(klass,start,limit,Order.asc(entityIdFieldName));
                
                TreeMap<String,Object> entityMap = new TreeMap<String,Object>();
                for (Object obj : entities) {
                    entityMap.put(marshaller.extractId(obj).toString(),obj);
                }
                
                request.setAttribute("start",start);
                request.setAttribute("limit",limit);
                
                request.setAttribute("total",controller.count(klass));
                
                request.setAttribute("simpleName",klass.getSimpleName());
                request.setAttribute("package",klass.getPackage().getName() + ".");
                request.setAttribute("entities",entityMap);
                
                if (ServletUtils.getBooleanParameter(request,"selector",false))
                    rd = request.getRequestDispatcher(DISPATCH_ENTITY_SELECTOR);
                else
                    rd = request.getRequestDispatcher(DISPATCH_ENTITY_LIST);
                
            } catch (Exception e) {
                log.error("Error listing entities",e);
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                return;
            }
        } else if (path[0].equals("find") || path[0].equals("inspect")) {
            log.debug("in find");
            if (path.length < 3) {
                log.debug("too few parts for find");
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
            try {
                Class klass = entityLookup.get(path[1]);
                
                if (klass == null) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    response.getWriter().println("Could not find class for entity type: " + path[1]);
                    return;
                }
                
                Marshaller marshaller = Marshaller.getForClass(klass);
                
                Object entity = controller.findPrimaryKey(klass,marshaller.asEntityIdType(path[2]));
                request.setAttribute("entity",entity);
                request.setAttribute("entityId",marshaller.extractId(entity));
                request.setAttribute("fields",marshaller.marshallOut(entity,true));
                
                if (path[0].equals("find"))
                    rd = request.getRequestDispatcher(DISPATCH_ENTITY_EDIT);
                else
                    rd = request.getRequestDispatcher(DISPATCH_ENTITY_INSPECT);
                
            } catch (Exception e) {
                log.error("error finding entity",e);
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                return;
            }
        } else if (path[0].equals("create")) {
            log.debug("in create");
            if (path.length < 2) {
                log.debug("too few parts for create");
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
            try {
                Class klass = entityLookup.get(path[1]);
                
                if (klass == null) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    response.getWriter().println("Could not find class for entity type: " + path[1]);
                    return;
                }
                                
                Marshaller marshaller = Marshaller.getForClass(klass);
                
                Object entity = klass.newInstance();
                request.setAttribute("simpleName",klass.getSimpleName());
                request.setAttribute("package",klass.getPackage().getName() + ".");
                request.setAttribute("fields",marshaller.marshallOut(entity,true));
                
                rd = request.getRequestDispatcher(DISPATCH_ENTITY_CREATE);
                
            } catch (Exception e) {
                log.error("error creating entity",e);
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                return;
            }
        } else if (path[0].equals("save")) {
            log.debug("in save");
            
            if (path.length < 2) {
                log.debug("too few parts for save");
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
            
            Marshaller marshaller = null;
            
            try {
                Class klass = entityLookup.get(path[1]);

                if (klass == null) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    response.getWriter().println("Could not find class for entity type: " + path[1]);
                    return;
                }
                
                marshaller = Marshaller.getForClass(klass);
                
                Object entity = klass.newInstance();
                
                entity = marshaller.marshallIn(entity,request.getParameterMap(),controller);
                
                controller.persist(entity,true);
                
                if (controller.hasErrors()) {
                    response.getWriter().println(controller.getErrorsAsString("<br />",true));
                    return;
                } else {
                    response.setStatus(HttpServletResponse.SC_CREATED);
                    
                    // need this to display dates in the DB stored format
                    entity = controller.findPrimaryKey(klass,marshaller.extractId(entity));
                    
                    request.setAttribute("entity",entity);
                    request.setAttribute("entityId",marshaller.extractId(entity));
                    request.setAttribute("fields",marshaller.marshallOut(entity,true));
                    
                    rd = request.getRequestDispatcher(DISPATCH_ENTITY_EDIT);
                }
                
            } catch (MarshallingException e) {
                log.error("error saving entity",e);
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().println(e.getMessage());
                return;
            } catch (Exception e) {
                log.error("error saving entity",e);
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                return;
            }

        } else if (path[0].equals("update")) {
            log.debug("in update");
            
            if (path.length < 3) {
                log.debug("too few parts for update");
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
            
            try {
                Class klass = entityLookup.get(path[1]);

                if (klass == null) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    response.getWriter().println("Could not find class for entity type: " + path[1]);
                    return;
                }
                
                Marshaller marshaller = Marshaller.getForClass(klass);
                
                Object entity = controller.findPrimaryKey(klass,marshaller.asEntityIdType(path[2]));
                
                entity = marshaller.marshallIn(entity,request.getParameterMap(),controller);
                
                controller.merge(entity);
                
                if (controller.hasErrors()) {
                    response.getWriter().println(controller.getErrorsAsString("<br />",true));
                    return;
                } else {
                    response.setStatus(HttpServletResponse.SC_CREATED); 
                    
                    // need this to display dates in the DB stored format
                    entity = controller.findPrimaryKey(klass,marshaller.extractId(entity));
                    
                    request.setAttribute("entity",entity);
                    request.setAttribute("entityId",marshaller.extractId(entity));
                    request.setAttribute("fields",marshaller.marshallOut(entity,true));
                    
                    rd = request.getRequestDispatcher(DISPATCH_ENTITY_EDIT);
                }
                
            } catch (MarshallingException e) {
                log.error("error saving entity",e);
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().println(e.getMessage());
                return;
            } catch (Exception e) {
                log.error("error updating entity", e);
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                return;
            }
            
        } else if (path[0].equals("delete")) {

            log.debug("in delete");
            
            if (path.length < 3) {
                log.debug("too few parts for delete");
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
            
            try {
                Class klass = entityLookup.get(path[1]);

                if (klass == null) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    response.getWriter().println("Could not find class for entity type: " + path[1]);
                    return;
                }
                
                Marshaller marshaller = Marshaller.getForClass(klass);
                
                Object entity = controller.findPrimaryKey(klass,marshaller.asEntityIdType(path[2]));
                
                controller.remove(entity);
                entity = null;
                controller.flush();
                
                if (controller.hasErrors()) {
                    response.getWriter().println(controller.getErrorsAsString("<br />",true));
                    return;
                } else {
                    response.setStatus(HttpServletResponse.SC_CREATED);
                    return;
                }
                
            } catch (Exception e) {
                log.error("error updating entity", e);
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                return;
            }
            
            
        } else {
            log.debug("Don't know what to do!");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        
        CharArrayWrapper responseWrapper = new CharArrayWrapper((HttpServletResponse)response);
        rd.forward(request,responseWrapper);
        
        String responseString = responseWrapper.toString().replaceAll("\n"," ").replaceAll("\\s{2,}"," ").replaceAll("> ",">").replaceAll(" <","<").replaceAll(" />","/>");
        response.setContentLength(responseString.length());
        response.setContentType("text/javascript");
        
        PrintWriter out = response.getWriter();
        out.write(responseString);
        out.flush();
        out.close();
    }
    
    private void snoopEntities() {
        /* This would look up all loaded classes, then filter down to whatever
         * you want.  It's not the best way for this process, but still pretty
         * fucking cool.
        try {
            ClassLoader cl = J2FreeAdminServlet.class.getClassLoader();
            Class clClass  = cl.getClass();
            while (clClass != java.lang.ClassLoader.class) {
                clClass = clClass.getSuperclass();
            }
            Field clClassesField = clClass.getDeclaredField("classes");
            clClassesField.setAccessible(true);
            Vector classes = (Vector)clClassesField.get(cl);
         
            Class klass;
            for (Iterator itr = classes.iterator(); itr.hasNext();) {
                klass = (Class)itr.next();
                if (klass.isAnnotationPresent(Entity.class))
                    availableEntities.add(klass);
            }
         
        } catch (Exception e) {
            log.error(e);
        }
         */

        Map metadata = Controller.get()
                                 .getSession()
                                 .getSessionFactory()
                                 .getAllClassMetadata();
        
        for (Iterator itr = metadata.entrySet().iterator(); itr.hasNext();) {
            try {
                Map.Entry ent = (Map.Entry)itr.next();
                Class clazz = Class.forName((String)ent.getKey());
                entityLookup.put(clazz.getSimpleName(),clazz);
            } catch (Exception e) {
                log.warn("Could not load " + itr.next());
            }
        }
    }
}