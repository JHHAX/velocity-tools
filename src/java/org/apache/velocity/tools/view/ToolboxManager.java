/*
 * Copyright 2003 The Apache Software Foundation.
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

package org.apache.velocity.tools.view;

import java.util.Map;
import org.apache.velocity.tools.view.context.ToolboxContext;

/**
 * Common interface for toolbox manager implementations.
 *
 * @author <a href="mailto:nathan@esha.com">Nathan Bubna</a>
 * @author <a href="mailto:geirm@apache.org">Geir Magnusson Jr.</a>
 * @author <a href="mailto:sidler@teamup.com">Gabe Sidler</a>
 * @version $Id: ToolboxManager.java,v 1.4 2004/11/11 04:03:38 nbubna Exp $
 */
public interface ToolboxManager
{

    /**
     * Adds a tool to be managed
     */
    void addTool(ToolInfo info);


    /**
     * @deprecated Use getToolbox(Object initData)
     */
    ToolboxContext getToolboxContext(Object initData);


    /**
     * Retrieves a map of the tools and data being managed. Tools
     * that implement the ViewTool interface will be (re)initialized
     * using the specified initData.
     *
     * @param initData data used to initialize ViewTools
     * @return the created ToolboxContext
     * @since VelocityTools 1.2
     */
    Map getToolbox(Object initData);

}
