# Manual Tests

## PinnedChildPages visual ordering

* Scenario
  * Create AWM Book application with a simple `title` field
  * Edit `BookTemplate` and add object `XWiki.PinnedChildPagesClass` to it
  * Create Book entry, eg "Alice in Wonderland", and a few child pages: "Down the Rabbit-Hole", "The Pool of Tears", "A
   Caucus-Race and a Long Tale"
  * Customize `BookSheet` as follows: 
    * Add `{{include reference="XWiki.PinnedChildPagesMacros"/}}` at the top
    * Add `#displaySortableChildren($doc.documentReference)` in the sheet form part
  * Edit page "Alice in Wonderland", reorder the subpages via drag and drop, save
* Expected result
  * Child pages can be reordered and the new order is saved in the `XWiki.PinnedChildPagesClass` object

## Tests setup

Use the following set up for each test case below:

* Create page P1
* Add child pages to P1
* Add PinnedPagesObject to P1, pointing at some of the child pages

Each test should be executed both with terminal and non-terminal pages.

## Pinned Page Removal

* Scenario: delete one of the P1 child pages that is pinned
* Expected result: P1 pinned pages does not contain the removed page anymore

## Not Pinned Child Page Removal

* Scenario: delete one of the P1 child pages that is not pinned
* Expected result: the page gets deleted successfully and the parent page pinned pages remains unchanged

## Pinned Page Move With Same Parent

* Scenario: rename one of the pinned pages of P1, still as a direct child of P1
* Expected result: the PinnedChildPages object of P1 contains the new name of the child page, at the same position in
 the list

## Pinned Page Move To New Parent

* Scenario: move one of the pinned pages of P1 to a different parent page
* Expected result: the renamed page gets removed from the P1's PinnedChildPages

