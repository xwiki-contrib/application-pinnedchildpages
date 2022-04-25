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
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.pinnedchildpages.PinnedChildPagesService;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.PageReference;
import org.xwiki.model.reference.PageReferenceResolver;
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

    @Inject
    private DocumentReferenceResolver<EntityReference> pageDocumentReferenceResolver;

    @Inject
    private PageReferenceResolver<EntityReference> documentPageReferenceResolver;

    /**
     * Indicates siblings traversal direction.
     */
    enum Direction
    {
        PREVIOUS,
        NEXT
    }

    @Override
    public List<EntityReference> getChildren(EntityReference reference) throws XWikiException
    {
        // First get pinned pages, if any
        List<EntityReference> orderedChildPages = getPinnedChildPages(reference);
        if (reference == null) {
            return orderedChildPages;
        }
        // Make sure to call the entityTreeNodeIdConverter with a DocumentReference otherwise it will fail
        EntityReference documentReference = reference;
        if (reference instanceof PageReference) {
            documentReference = getDocumentReference(reference);
        }

        // Then add non-pinned children pages except WebPreferences
        String name = entityTreeNodeIdConverter.convert(String.class, documentReference);
        int childCount = tree.getChildCount(name);
        List<String> childNodeIds = tree.getChildren(name, 0, childCount);
        for (String childNodeId : childNodeIds) {
            // FIXME: the conversion below does not produce valid DocumentReferences for non-terminal pages
            // EntityReference childReference = entityTreeNodeIdConverter.convert(EntityReference.class, childName);
            String childName = childNodeId.replace("document:", "");
            EntityReference childReference = documentReferenceResolver.resolve(childName);
            if (reference instanceof PageReference) {
                childReference = getPageReference(childReference);
            }
            if (orderedChildPages.indexOf(childReference) < 0 && !childReference.getName().equals("WebPreferences")) {
                orderedChildPages.add(childReference);
            }
        }
        return orderedChildPages;
    }

    @Override
    public List<EntityReference> getChildren(EntityReference reference, EntityReference xclass) throws XWikiException
    {
        List<EntityReference> children = getChildren(reference);
        if (xclass != null) {
            XWikiContext xcontext = xcontextProvider.get();
            XWiki xwiki = xcontext.getWiki();
            Predicate<EntityReference> hasXObject = childReference -> {
                try {
                    XWikiDocument childDocument = xwiki.getDocument(childReference, xcontext);
                    return childDocument.getXObject(xclass) != null;
                } catch (XWikiException e) {
                    // TODO: add logger
                    return false;
                }
            };
            return children.stream().filter(hasXObject).collect(Collectors.toList());
        } else {
            return children;
        }
    }

    /**
     * Converts a PageReference to a DocumentReference.
     *
     * @param reference a PageReference
     * @return the given reference converted to a DocumentReference
     */
    public DocumentReference getDocumentReference(EntityReference reference)
    {
        return pageDocumentReferenceResolver.resolve(reference);
    }

    @Override
    public EntityReference getNextSibling(EntityReference reference) throws XWikiException
    {
        return getSibling(reference, Direction.NEXT);
    }

    @Override
    public List<EntityReference> getNextSiblings(EntityReference reference) throws XWikiException
    {
        return getSiblings(reference, Direction.NEXT);
    }

    @Override
    public List<EntityReference> getNextSiblings(EntityReference reference, int limit) throws XWikiException
    {
        return getSiblings(reference, Direction.NEXT, limit);
    }

    /**
     * Converts a DocumentReference to a PageReference.
     *
     * @param reference a DocumentReference
     * @return the given reference converted to a PageReference
     */
    public PageReference getPageReference(EntityReference reference)
    {
        return documentPageReferenceResolver.resolve(reference);
    }

    @Override
    public List<EntityReference> getPinnedChildPages(EntityReference reference) throws XWikiException
    {
        // TODO: if the reference is a wiki reference, then the pinned child pages should be retrieved from the object
        // attached to XWiki.XWikiPreferences
        List<EntityReference> pinnedChildPageReferences = new ArrayList<>();
        if (reference == null) {
            return pinnedChildPageReferences;
        }
        XWikiContext xcontext = this.xcontextProvider.get();
        XWiki wiki = xcontext.getWiki();
        XWikiDocument doc = wiki.getDocument(reference, xcontext);
        BaseObject pinnedChildPagesObject = doc.getXObject(PINNED_CHILD_PAGES_CLASS_REFERENCE);
        // First add pinned pages, if any
        if (pinnedChildPagesObject != null) {
            List<String> pinnedChildPages = pinnedChildPagesObject.getListValue(PINNED_CHILD_PAGES_FIELD);
            if (pinnedChildPages != null && pinnedChildPages.size() > 0) {
                for (String pinnedChildPageName : pinnedChildPages) {
                    EntityReference pinnedChildPageReference =
                        documentReferenceResolver.resolve(pinnedChildPageName, reference);
                    if (reference instanceof PageReference) {
                        pinnedChildPageReference = getPageReference(pinnedChildPageReference);
                    }
                    pinnedChildPageReferences.add(pinnedChildPageReference);
                }
            }
        }
        return pinnedChildPageReferences;
    }

    @Override
    public EntityReference getPreviousSibling(EntityReference reference) throws XWikiException
    {
        return getSibling(reference, Direction.PREVIOUS);
    }

    @Override
    public List<EntityReference> getPreviousSiblings(EntityReference reference) throws XWikiException
    {
        return getSiblings(reference, Direction.PREVIOUS);
    }

    @Override
    public List<EntityReference> getPreviousSiblings(EntityReference reference, int limit) throws XWikiException
    {
        return getSiblings(reference, Direction.PREVIOUS, limit);
    }

    @Override
    public List<EntityReference> getSiblings(EntityReference reference) throws XWikiException
    {
        PageReference pageReference = getPageReference(reference);
        EntityReference parent = pageReference.getParent();
        if (reference instanceof DocumentReference) {
            parent = getDocumentReference(parent);
        }
        return getChildren(parent);
    }

    /**
     * Returns first sibling in given direction.
     *
     * @param reference a page
     * @param direction a direction
     * @return sibling
     * @throws XWikiException in case an error occurs
     */
    protected EntityReference getSibling(EntityReference reference, Direction direction)
        throws XWikiException
    {
        List<EntityReference> siblings = getSiblings(reference, direction);
        if (siblings.size() > 0) {
            return siblings.get(0);
        } else {
            return null;
        }
    }

    /**
     * Returns siblings in a given direction.
     *
     * @param reference a given reference
     * @param direction direction
     * @return siblings
     * @throws XWikiException in case an error occurs
     */
    public List<EntityReference> getSiblings(EntityReference reference, Direction direction)
        throws XWikiException
    {
        List<EntityReference> siblings = getSiblings(reference);
        int index = siblings.indexOf(reference);
        if (index >= 0) {
            if (direction.equals(Direction.NEXT) && index < siblings.size()) {
                return siblings.subList(index + 1, siblings.size());
            } else if (index >= 1) {
                List<EntityReference> list = siblings.subList(0, index);
                Collections.reverse(list);
                return list;
            }
        }
        return new ArrayList<>();
    }

    /**
     * Return siblings in given direction up to a limit.
     *
     * @param reference a page
     * @param direction direction
     * @param limit limit
     * @return siblings
     * @throws XWikiException in case an error occurs
     */
    public List<EntityReference> getSiblings(EntityReference reference, Direction direction, int limit)
        throws XWikiException
    {
        List<EntityReference> siblings = getSiblings(reference, direction);
        if (siblings.size() > limit) {
            return siblings.subList(0, limit);
        } else {
            return siblings;
        }
    }

    @Override
    public int indexOf(EntityReference reference) throws XWikiException
    {
        List<EntityReference> siblings = getSiblings(reference);
        return siblings.indexOf(reference);
    }
}
