/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2003 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "The Jakarta Project", "Velocity", and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */


package org.apache.velocity.tools.view.servlet;


import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;

import java.util.List;
import java.util.Iterator;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;

import javax.servlet.http.HttpSession;
import javax.servlet.ServletContext;

import org.apache.commons.digester.Digester;
import org.apache.commons.digester.RuleSet;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.tools.view.DataInfo;
import org.apache.velocity.tools.view.ToolInfo;
import org.apache.velocity.tools.view.XMLToolboxManager;
import org.apache.velocity.tools.view.context.ToolboxContext;
import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.servlet.ServletToolboxRuleSet;


/**
 * <p>A toolbox manager for the servlet environment.</p>
 *
 * <p>A toolbox manager is responsible for automatically filling the Velocity
 * context with a set of view tools. This class provides the following 
 * features:</p>
 * <ul>
 *   <li>configurable through an XML-based configuration file</li>   
 *   <li>assembles a set of view tools (the toolbox) on request</li>
 *   <li>handles different tool scopes (request, session, application)</li>
 *   <li>supports any class with a public constructor without parameters 
 *     to be used as a view tool</li>
 *   <li>supports adding primitive data values to the context(String,Number,Boolean)</li>
 * </ul>
 * 
 *
 * <p><strong>Configuration</strong></p>
 * <p>The toolbox manager is configured through an XML-based configuration
 * file. The configuration file is passed to the {@link #load(java.io.InputStream input)}
 * method. The format is shown in the following example:</p>
 * <pre> 
 * &lt;?xml version="1.0"?&gt;
 * 
 * &lt;toolbox&gt;
 *   &lt;tool&gt;
 *      &lt;key&gt;link&lt;/key&gt;
 *      &lt;scope&gt;request&lt;/scope&gt;
 *      &lt;class&gt;org.apache.velocity.tools.view.tools.LinkTool&lt;/class&gt;
 *   &lt;/tool&gt;
 *   &lt;tool&gt;
 *      &lt;key&gt;date&lt;/key&gt;
 *      &lt;scope&gt;application&lt;/scope&gt;
 *      &lt;class&gt;org.apache.velocity.tools.generic.DateTool&lt;/class&gt;
 *   &lt;/tool&gt;
 *   &lt;data type="number"&gt;
 *      &lt;key&gt;luckynumber&lt;/key&gt;
 *      &lt;value&gt;1.37&lt;/value&gt;
 *   &lt;/data&gt;
 *   &lt;data type="string"&gt;
 *      &lt;key&gt;greeting&lt;/key&gt;
 *      &lt;value&gt;Hello World!&lt;/value&gt;
 *   &lt;/data&gt;
 *   &lt;xhtml&gt;true&lt;/xhtml&gt;
 * &lt;/toolbox&gt;    
 * </pre>
 * <p>The recommended location for the configuration file is the WEB-INF directory of the
 * web application.</p>
 *
 * @author <a href="mailto:sidler@teamup.com">Gabriel Sidler</a>
 * @author <a href="mailto:nathan@esha.com">Nathan Bubna</a>
 * @author <a href="mailto:geirm@apache.org">Geir Magnusson Jr.</a>
 *
 * @version $Id: ServletToolboxManager.java,v 1.10 2004/02/12 18:13:21 nbubna Exp $
 */
public class ServletToolboxManager extends XMLToolboxManager
{

    // --------------------------------------------------- Properties ---------

    public static final String SESSION_TOOLS_KEY = 
        "org.apache.velocity.tools.view.tools.ServletToolboxManager.SessionTools";

    private ServletContext servletContext;
    private Map appTools;
    private ArrayList sessionToolInfo;
    private ArrayList requestToolInfo;
    private boolean createSession;

    private static HashMap managersMap = new HashMap();
    private static RuleSet servletRuleSet = new ServletToolboxRuleSet();


    // --------------------------------------------------- Constructor --------

    /**
     * Use getInstance(ServletContext,String) instead
     * to ensure there is exactly one ServletToolboxManager
     * per xml toolbox configuration file.
     */
    private ServletToolboxManager(ServletContext servletContext)
    {
        this.servletContext = servletContext;
        appTools = new HashMap();
        sessionToolInfo = new ArrayList();
        requestToolInfo = new ArrayList();
        createSession = true;
    }


    // -------------------------------------------- Public Methods ------------

    /**
     * ServletToolboxManager factory method.
     * This method will ensure there is exactly one ServletToolboxManager
     * per xml toolbox configuration file.
     */
    public static synchronized ServletToolboxManager getInstance(ServletContext servletContext,
                                                                 String toolboxFile)
    {
        // little fix up
        if (!toolboxFile.startsWith("/"))
        {
            toolboxFile = "/" + toolboxFile;
        }

        // get config file pathname
        String pathname = servletContext.getRealPath(toolboxFile);

        // check if a previous instance exists
        ServletToolboxManager toolboxManager = 
            (ServletToolboxManager)managersMap.get(pathname);

        if (toolboxManager == null)
        {
            // if not, build one
            InputStream is = null;
            try
            {
                // get the bits
                is = servletContext.getResourceAsStream(toolboxFile);

                if (is != null)
                {
                    Velocity.info("ServletToolboxManager: Using config file '" + 
                                  toolboxFile +"'");

                    toolboxManager = new ServletToolboxManager(servletContext);
                    toolboxManager.load(is);

                    // remember it
                    managersMap.put(pathname, toolboxManager);

                    Velocity.info("ServletToolboxManager: Toolbox setup complete.");
                }
            }
            catch(Exception e)
            {
                Velocity.error("Problem loading toolbox '" + toolboxFile +"' : " + e);

                // if this happens, it probably deserves
                // to have the stack trace logged
                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                Velocity.error(sw.toString());
            }
            finally
            {
                try
                {
                    if (is != null)
                    {
                        is.close();
                    }
                }
                catch(Exception ee) {}
            }
        }
        return toolboxManager;
    }


    /**
     * <p>Sets whether or not to create a new session when none exists for the
     * current request and session-scoped tools have been defined for this
     * toolbox.</p>
     *
     * <p>If true, then a call to {@link #getToolboxContext(Object)} will 
     * create a new session if none currently exists for this request and
     * the toolbox has one or more session-scoped tools designed.</p>
     *
     * <p>If false, then a call to getToolboxContext(Object) will never
     * create a new session for the current request.
     * This effectively means that no session-scoped tools will be added to 
     * the ToolboxContext for a request that does not have a session object.
     * </p>
     *
     * The default value is true.
     */
    public void setCreateSession(boolean b)
    {
        createSession = b;
        Velocity.debug("ServletToolboxManager: create-session is set to " + b);
    }


    /**
     * <p>Sets an application attribute to tell velocimacros and tools 
     * (especially the LinkTool) whether they should output XHTML or HTML.</p>
     *
     * @see ViewContext#XHTML
     * @since VelocityTools 1.1
     */
    public void setXhtml(Boolean value)
    {
        servletContext.setAttribute(ViewContext.XHTML, value);
        Velocity.info("ServletToolboxManager: " + ViewContext.XHTML + 
                      " is set to " + value);
    }


    // ------------------------------ XMLToolboxManager Overrides -------------

    /**
     * For subclassing convienence.
     * @since VelocityTools 1.1
     */
    protected RuleSet getRuleSet()
    {
        return servletRuleSet;
    }


    /**
     * Overrides XMLToolboxManager to separate tools by scope.
     * For this to work, we obviously override getToolboxContext(Object) as well.
     */
    public void addTool(ToolInfo info)
    {
        if (info instanceof DataInfo)
        {
            //add static data to the appTools map
            appTools.put(info.getKey(), info.getInstance(null));
        }
        else if (info instanceof ServletToolInfo)
        {
            ServletToolInfo sti = (ServletToolInfo)info;
            
            if (ViewContext.REQUEST.equalsIgnoreCase(sti.getScope()))
            {
                requestToolInfo.add(sti);
            }
            else if (ViewContext.SESSION.equalsIgnoreCase(sti.getScope()))
            {
                sessionToolInfo.add(sti);
            }
            else if (ViewContext.APPLICATION.equalsIgnoreCase(sti.getScope()))
            {
                /* add application scoped tools to appTools and
                 * initialize them with the ServletContext */
                appTools.put(sti.getKey(), sti.getInstance(servletContext));
            }
            else
            {
                Velocity.warn("ServletToolboxManager: Unknown scope '" +
                              sti.getScope() + "' - " + sti.getKey() + 
                              " will be request scoped.");
                requestToolInfo.add(sti);
            }
        }
        else
        {
            //default is request scope
            requestToolInfo.add(info);
        }
    }


    /**
     * Overrides XMLToolboxManager to handle the separate
     * scopes.
     *
     * Application scope tools were initialized when the toolbox was loaded.
     * Session scope tools are initialized once per session and stored in a
     * map in the session attributes.
     * Request scope tools are initialized on every request.
     * 
     * @param initData the {@link ViewContext} for the current servlet request
     */
    public ToolboxContext getToolboxContext(Object initData)
    {
        //we know the initData is a ViewContext
        ViewContext ctx = (ViewContext)initData;
        
        //create the toolbox map with the application tools in it
        Map toolbox = new HashMap(appTools);

        if (!sessionToolInfo.isEmpty())
        {
            HttpSession session = ctx.getRequest().getSession(createSession);
            if (session != null)
            {
                // allow only one thread per session at a time
                synchronized(getMutex(session))
                {
                    // get the session tools
                    Map stmap = (Map)session.getAttribute(SESSION_TOOLS_KEY);
                    if (stmap == null)
                    {
                        // init and store session tools map
                        stmap = new HashMap(sessionToolInfo.size());
                        Iterator i = sessionToolInfo.iterator();
                        while(i.hasNext())
                        {
                            ToolInfo ti = (ToolInfo)i.next();
                            stmap.put(ti.getKey(), ti.getInstance(ctx));
                        }
                        session.setAttribute(SESSION_TOOLS_KEY, stmap);
                    }
                    // add them to the toolbox
                    toolbox.putAll(stmap);
                }
            }
        }

        //add and initialize request tools
        Iterator i = requestToolInfo.iterator();
        while(i.hasNext())
        {
            ToolInfo info = (ToolInfo)i.next();
            toolbox.put(info.getKey(), info.getInstance(ctx));
        }

        return new ToolboxContext(toolbox);
    }


    /**
     * Returns a mutex (lock object) unique to the specified session 
     * to allow for reliable synchronization on the session.
     */
    protected Object getMutex(HttpSession session)
    {
        // yes, this uses double-checked locking, but it is safe here
        // since partial initialization of the lock is not an issue
        Object lock = session.getAttribute("session.mutex");
        if (lock == null)
        {
            // one thread per toolbox manager at a time
            synchronized(this)
            {
                // in case another thread already came thru
                lock = session.getAttribute("session.mutex");
                if (lock == null)
                {
                    lock = new Object();
                    session.setAttribute("session.mutex", lock);
                }
            }
        }
        return lock;
    }

}
