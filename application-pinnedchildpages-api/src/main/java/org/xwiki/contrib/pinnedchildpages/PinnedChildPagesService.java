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
package org.xwiki.contrib.pinnedchildpages;

import java.util.List;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.LocalDocumentReference;

import com.xpn.xwiki.XWikiException;

/**
 * Service easing manipulation of pinned child pages.
 *
 * @version $Id$
 */
@Role
public interface PinnedChildPagesService
{
    /**
     * Reference to XWiki Class PinnedChildPagesClass.
     */
    EntityReference
        PINNED_CHILD_PAGES_CLASS_REFERENCE = new LocalDocumentReference("XWiki", "PinnedChildPagesClass");
    /**
     * Name of the field storing the list of pinned child pages.
     */
    String PINNED_CHILD_PAGES_FIELD = "pinnedChildPages";

    /**
     * Returns the list of child pages of a given reference with ordered pinned pages first, then other children in
     * default order, and excluding the WebPreferences child page.
     *
     * @param reference a reference to a page
     * @return list of child pages
     * @throws XWikiException in case an error occurs
     */
    List<EntityReference> getChildren(EntityReference reference) throws XWikiException;

    /**
     * Returns first next sibling.
     * @param reference a page
     * @return sibling
     * @throws XWikiException in case an error occurs
     */
    EntityReference getNextSibling(EntityReference reference) throws XWikiException;

    /**
     * Returns references to the next siblings of a given page.
     * @param reference a reference to a page
     * @return list of next child pages
     * @throws XWikiException in case an error occurs
     */
    List<EntityReference> getNextSiblings(EntityReference reference) throws XWikiException;

    /**
     * Returns references to the next siblings of a given page, up to a limit.
     * @param reference a reference to a page
     * @param limit maximum number of entries to be returned
     * @return list of next child pages
     * @throws XWikiException in case an error occurs
     */
    List<EntityReference> getNextSiblings(EntityReference reference, int limit) throws XWikiException;

    /**
     * Returns references to the pinned child pages of a given page.
     * @param reference a reference to a page
     * @return list of pinned child pages references
     * @throws XWikiException in case an error occurs
     */
    List<EntityReference> getPinnedChildPages(EntityReference reference) throws XWikiException;

    /**
     * Returns first previous sibling.
     * @param reference a page
     * @return previous sibling
     * @throws XWikiException in case an error occurs
     */
    EntityReference getPreviousSibling(EntityReference reference) throws XWikiException;

    /**
     * Returns references to the previous siblings of a given page.
     * @param reference a reference to a page
     * @return list of previous pinned pages
     * @throws XWikiException in case an error occurs
     */
    List<EntityReference> getPreviousSiblings(EntityReference reference) throws XWikiException;

    /**
     * Returns references to the previous siblings of a given page, up to a limit.
     * @param reference a reference to a page
     * @param limit maximum number of entries to be returned
     * @return list of previous pinned pages
     * @throws XWikiException in case an error occurs
     */
    List<EntityReference> getPreviousSiblings(EntityReference reference, int limit) throws XWikiException;

    /**
     * Returns index of reference among pinned child pages.
     * @param reference a page
     * @return page index in pinned pages
     * @throws XWikiException in case of error
     */
    int indexOf(EntityReference reference) throws XWikiException;


    /**
     * Returns ordered siblings of a page, including the page itself.
     * @param reference a page
     * @return list of siblings
     * @throws XWikiException in case of error
     */
    List<EntityReference> getSiblings(EntityReference reference) throws XWikiException;


}
