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

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.job.event.JobStartedEvent;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.model.reference.PageReference;
import org.xwiki.model.reference.PageReferenceResolver;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.ObservationContext;
import org.xwiki.observation.event.Event;
import org.xwiki.refactoring.event.DocumentRenamedEvent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Listener updating PinnedChildPages object when a pinned page gets renamed or deleted.
 *
 * @version $Id$
 */
@Component
@Named(PinnedChildPagesUpdaterListener.LISTENER_NAME)
@Singleton
public class PinnedChildPagesUpdaterListener extends AbstractEventListener
{
    /**
     * The name of the event listener.
     */
    public static final String LISTENER_NAME = "pinnedChildPages.listener";

    /**
     * Reference to XWiki Class PinnedChildPagesClass.
     */
    public static final LocalDocumentReference
        PINNED_CHILD_PAGES_CLASS_REFERENCE = new LocalDocumentReference("XWiki", "PinnedChildPagesClass");

    /**
     * Name of the field storing the list of pinned child pages.
     */
    public static final String PINNED_CHILD_PAGES_FIELD = "pinnedChildPages";

    @Inject
    protected Logger logger;

    @Inject
    @Named("compactwiki")
    protected EntityReferenceSerializer<String> compactWikiSerializer;

    @Inject
    protected Provider<XWikiContext> contextProvider;

    @Inject
    protected ObservationContext observationContext;

    @Inject
    protected PageReferenceResolver<EntityReference> pageReferenceResolver;

    /**
     * This is the default constructor.
     */
    public PinnedChildPagesUpdaterListener()
    {
        super(LISTENER_NAME, new DocumentRenamedEvent(), new DocumentDeletedEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        logger.debug("[%s] - Event: [%s] - Source: [%s] - Data: [%s]", LISTENER_NAME, event, source, data);

        if (event instanceof DocumentRenamedEvent) {
            DocumentReference originalReference = ((DocumentRenamedEvent) event).getSourceReference();
            DocumentReference newReference = ((DocumentRenamedEvent) event).getTargetReference();
            try {
                this.handlePinnedChildPageMove(originalReference, newReference);
            } catch (XWikiException e) {
                logger.error("An error occurred while processing pinned child page move from [%s] to [%s]",
                    originalReference, newReference, e);
            }
        } else if (event instanceof DocumentDeletedEvent) {

            boolean isRename = observationContext.isIn(new JobStartedEvent("refactoring/rename"));
            if (isRename) {
                return;
            }
            DocumentReference reference = ((XWikiDocument) source).getDocumentReference();
            try {
                this.maybeUnpinChildPage(reference);
            } catch (Exception e) {
                logger.error("Error while processing pinned child page removal: [%s]", reference, e);
            }
        }
    }

    /**
     * Handles pinned child page move.
     *
     * @param originalReference original page reference
     * @param newReference new page reference after move
     * @throws XWikiException if an error occurs
     */
    public void handlePinnedChildPageMove(DocumentReference originalReference, DocumentReference newReference)
        throws XWikiException
    {
        XWikiContext context = contextProvider.get();
        XWiki wiki = context.getWiki();
        EntityReference originalParentReference = getParentReference(originalReference);

        // Check whether the parent of the moved page has a PINNED_CHILD_PAGES_CLASS object
        XWikiDocument parentPage = wiki.getDocument(originalParentReference, context).clone();
        BaseObject pinnedChildPagesObject = parentPage.getXObject(PINNED_CHILD_PAGES_CLASS_REFERENCE);
        if (pinnedChildPagesObject != null) {
            EntityReference newParentReference = getParentReference(newReference);
            // If page kept the same parent, just update its reference in the parent's pinned pages, otherwise
            // remove it from its original parent pinned pages.
            if (originalParentReference.equals(newParentReference)) {
                List pinnedChildPages = pinnedChildPagesObject.getListValue(PINNED_CHILD_PAGES_FIELD);
                String originalId = compactWikiSerializer.serialize(originalReference);
                String newId = compactWikiSerializer.serialize(newReference);
                int index = pinnedChildPages.indexOf(originalId);
                if (index >= 0) {
                    pinnedChildPages.set(index, newId);
                    String name = getName(newReference);
                    wiki.saveDocument(parentPage, String.format("Updated pinned child pages after rename of %s", name),
                        true, context);
                }
            } else {
                maybeUnpinChildPage(originalReference);
            }
        }
    }

    /**
     * Removes a page reference from its parent pinned child pages in case the parent has pinned child pages and the
     * given reference if is part of them.
     *
     * @param reference removed page reference
     * @throws XWikiException if an error occurs
     */
    public void maybeUnpinChildPage(DocumentReference reference) throws XWikiException
    {
        XWikiContext context = contextProvider.get();
        XWiki wiki = context.getWiki();
        EntityReference parentReference = getParentReference(reference);
        XWikiDocument parentPage = wiki.getDocument(parentReference, context).clone();
        BaseObject pinnedChildPagesObject = parentPage.getXObject(PINNED_CHILD_PAGES_CLASS_REFERENCE);
        if (pinnedChildPagesObject != null) {
            List pinnedChildPages = pinnedChildPagesObject.getListValue(PINNED_CHILD_PAGES_FIELD);
            String deletedPageId = compactWikiSerializer.serialize(reference);
            boolean result = pinnedChildPages.remove(deletedPageId);
            if (result) {
                pinnedChildPagesObject.setDBStringListValue(PINNED_CHILD_PAGES_FIELD, pinnedChildPages);
                String name = getName(reference);
                wiki.saveDocument(parentPage,
                    String.format("Removed subpage %s from pinned child pages list", name), true,
                    context);
            }
        }
    }

    /**
     * Converts a given DocumentReference to a PageReference and returns its parent.
     *
     * @param reference a reference
     * @return the parent reference
     */
    public EntityReference getParentReference(DocumentReference reference)
    {
        PageReference pageReference = pageReferenceResolver.resolve(reference);
        return pageReference.getParent();
    }

    /**
     * Converts a given DocumentReference into a PageReference and returns its name. This allows to obtain a short name
     * for a given reference, without the default part.
     *
     * @param reference a DocumentReference
     * @return reference short name
     */
    public String getName(DocumentReference reference)
    {
        return pageReferenceResolver.resolve(reference).getName();
    }
}
