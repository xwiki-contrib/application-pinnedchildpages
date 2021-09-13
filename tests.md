# Manual Tests

## PinnedChildPages visual ordering

* Scenario
  * Create AWM application with a simple "Title" field, eg Book
  * Create Book entry, eg "Alice in Wonderland", and a few subpages: Down the Rabbit-Hole, The Pool of Tears, A Caucus-Race and a Long Tale
  * Customize BookSheet as follows: 
    * Add `{{include reference="XWiki.PinnedChildPagesMacros"/}}` at the top
    * Add `#displaySortableChildPages($doc.documentReference)` in the sheet form part
  * Edit page "Alice in Wonderland", reorder the subpages, save
* Expected result
  * Child pages can be reordered and the new order can be saved

## Tests setup

Use the following set up for each test case below:

* Create page P1
* Add children page to P1
* Add PinnedPagesObject to P1, pointing at some of the children pages

Each test should be executed both with terminal and non-terminal pages.

## Pinned Page Removal

* Scenario: remove one of the P1 children pages that is pinned
* Expected result: P1 pinned pages does not contain the removed page anymore

## Pinned Page Move With Same Parent

* Scenario: rename one of the pinned pages of P1, still as a direct child of P1
* Expected result: the PinnedChildPages object of P1 contains the new name of the child page, at the same position in
 the list

## Pinned Page Move To New Parent

* Scenario: move one of the pinned pages of P1 to a different parent page
* Expected result: the renamed page gets removed from the P1's PinnedChildPages

