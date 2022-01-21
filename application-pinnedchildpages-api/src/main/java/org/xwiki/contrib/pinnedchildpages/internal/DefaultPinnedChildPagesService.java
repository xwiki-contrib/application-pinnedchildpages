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
package org.xwiki.contrib.pinnedchildpages.internal;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.pinnedchildpages.PinnedChildPagesService;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.properties.converter.Converter;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Default implementation of PinnedChildPagesService.
 *
 * @version $Id$
 */
@Component
@Singleton
public class DefaultPinnedChildPagesService implements PinnedChildPagesService
{
    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Inject
    @Named("nestedPages")
    private org.xwiki.tree.Tree tree;

    @Inject
    @Named("entityTreeNodeId")
    private Converter<EntityReference> entityTreeNodeIdConverter;

    @Override
    public List<EntityReference> getChildren(EntityReference reference) throws XWikiException
    {
        // First get pinned pages, if any
        List<EntityReference> orderedChildPages = getPinnedChildPages(reference);

        // Then add non-pinned children pages except WebPreferences
        String name = entityTreeNodeIdConverter.convert(String.class, reference);
        int childCount = tree.getChildCount(name);
        List<String> childNodeIds = tree.getChildren(name, 0, childCount);
        for (String childNodeId : childNodeIds) {
            // FIXME: the conversion below does not produce valid DocumentReferences for non-terminal pages
            // EntityReference childReference = entityTreeNodeIdConverter.convert(EntityReference.class, childName);
            String childName = childNodeId.replace("document:", "");
            DocumentReference childReference = documentReferenceResolver.resolve(childName);
            if (orderedChildPages.indexOf(childReference) < 0 && !childReference.getName().equals("WebPreferences")) {
                orderedChildPages.add(childReference);
            }
        }
        return orderedChildPages;
    }

    @Override
    public List<EntityReference> getPinnedChildPages(EntityReference reference) throws XWikiException
    {
        XWikiContext xcontext = this.xcontextProvider.get();
        XWiki wiki = xcontext.getWiki();
        List<EntityReference> pinnedChildPageReferences = new ArrayList<>();
        XWikiDocument doc = wiki.getDocument(reference, xcontext);
        BaseObject pinnedChildPagesObject = doc.getXObject(PINNED_CHILD_PAGES_CLASS_REFERENCE);
        // First add pinned pages, if any
        if (pinnedChildPagesObject != null) {
            List<String> pinnedChildPages = pinnedChildPagesObject.getListValue(PINNED_CHILD_PAGES_FIELD);
            if (pinnedChildPages != null && pinnedChildPages.size() > 0) {
                for (String pinnedChildPageName : pinnedChildPages) {
                    DocumentReference pinnedChildPageReference = documentReferenceResolver.resolve(pinnedChildPageName);
                    pinnedChildPageReferences.add(pinnedChildPageReference);
                }
            }
        }
        return pinnedChildPageReferences;
    }
}
