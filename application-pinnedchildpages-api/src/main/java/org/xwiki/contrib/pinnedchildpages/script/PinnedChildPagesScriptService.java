/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.contrib.pinnedchildpages.script;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.pinnedchildpages.PinnedChildPagesService;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.script.service.ScriptService;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;

/**
 * ScriptService easing manipulation of pinned child pages.
 *
 * @version $Id$
 */
@Component
@Named("pinnedChildPages")
@Singleton
public class PinnedChildPagesScriptService implements ScriptService
{
    private static final String ERROR_MESSAGE = "message";

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private Logger logger;

    @Inject
    private PinnedChildPagesService pinnedChildPagesService;

    /**
     * Returns the list of child pages of a given reference with ordered pinned pages first, then other children in
     * default order, and excluding the WebPreferences child page.
     *
     * @param reference a page reference
     * @return list of child pages
     */
    public List<EntityReference> getChildren(EntityReference reference)
    {
        try {
            return pinnedChildPagesService.getChildren(reference);
        } catch (XWikiException e) {
            xcontextProvider.get().put(ERROR_MESSAGE, e.toString());
            logger.error(e.toString(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Returns a list of references to the pinned child pages of a given page reference.
     *
     * @param reference a reference to a page
     * @return list of pinned child page references
     */
    public List<EntityReference> getPinnedChildPages(EntityReference reference)
    {
        try {
            return pinnedChildPagesService.getPinnedChildPages(reference);
        } catch (XWikiException e) {
            xcontextProvider.get().put(ERROR_MESSAGE, e.toString());
            logger.error(e.toString(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Returns next siblings of a given page.
     * @param reference a reference to a page
     * @return list of next siblings
     */
    public List<EntityReference> getNextSiblings(EntityReference reference)
    {
        try {
            return pinnedChildPagesService.getNextSiblings(reference);
        } catch (XWikiException e) {
            xcontextProvider.get().put(ERROR_MESSAGE, e.toString());
            logger.error(e.toString(), e);
            return new ArrayList<>();
        }
    }
}
