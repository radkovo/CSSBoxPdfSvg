package org.fit.cssbox.svgpdf.render;

import cz.vutbr.web.css.CSSProperty;
import cz.vutbr.web.css.NodeData;
import org.fit.cssbox.layout.Box;
import org.fit.cssbox.layout.ElementBox;
import org.fit.cssbox.layout.ReplacedBox;
import org.fit.cssbox.layout.TextBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * Class for create break table and avoid table for paging PDF.
 *
 * @author Tomas Chocholaty
 */
public class BreakAvoidTables {


    private Node rootNodeOfTree, recentNodeInTree, rootNodeOfList, recentNodeInList;
    private List<Node> nodesWithoutParent = new ArrayList<>(16);

    PDFRenderer pdfRenderer;

    public BreakAvoidTables(PDFRenderer pdfRenderer) {
        this.pdfRenderer = pdfRenderer;
    }

    public Node getRootNodeOfTree() {
        return rootNodeOfTree;
    }

    public Node getRecentNodeInTree() {
        return recentNodeInTree;
    }


    public List<Node> getNodesWithoutParent() {
        return nodesWithoutParent;
    }

    public void setRootNodeOfTree(Node rootNodeOfTree) {
        this.rootNodeOfTree = rootNodeOfTree;
    }

    public void setRecentNodeInTree(Node recentNodeInTree) {
        this.recentNodeInTree = recentNodeInTree;
    }

    public void setRecentNodeInList(Node recentNodeInList) {
        this.recentNodeInList = recentNodeInList;
    }


    // break/avoid tables
    private List<float[]> breakTable = new ArrayList<>(2);
    private List<float[]> avoidTable = new ArrayList<>(2);

    /**
     * Find previous Node for insert current node as child
     *
     * @param parentID - order of parent element
     * @param myID     - order of current element
     */
    private Node findNodeToInsert(int parentID, int myID) {

        // wanted node "to insert" is recent node
        if (getRecentNodeInTree().getID() == parentID)
            return getRecentNodeInTree();

        // there is 2x ID=0 at the root of TREE - if my parents ID is zero and I
        // am not,
        // I have to insert to the second node with ID=0
        Node a;
        if (myID != 0 && parentID == 0) {
            return getRootNodeOfTree().getAllChildren().firstElement();
        }

        // wanted node "to insert" is parent node of recent node
        if (getRecentNodeInTree().getParentNode() != null && getRecentNodeInTree().getParentNode().getID() == parentID)
            return getRecentNodeInTree().getParentNode();

        // goes through whole tree
        List<Node> queueOpen = new ArrayList<>(16);
        queueOpen.add(getRootNodeOfTree());
        while (queueOpen.size() > 0) {
            if (queueOpen.get(0).getID() == parentID)
                return queueOpen.get(0);

            List<Node> children = queueOpen.get(0).getAllChildren();
            if (children != null)
                queueOpen.addAll(children);
            queueOpen.remove(0);
        }
        return null;
    }

    /**
     * create tree of element as nodes tree
     *
     * @param elem - the element
     */
    public void createNodesTree(ElementBox elem) {
        if (elem.getParent() == null) {
            setRootNodeOfTree(new Node(null, elem, null, null, null));
            setRecentNodeInTree(getRootNodeOfTree());
            for (int i = 0; i < elem.getSubBoxNumber(); i++) {
                createNodesTree((ElementBox) elem.getSubBox(i));
            }

        } else {
            Node targetNode = findNodeToInsert(elem.getParent().getOrder(), elem.getOrder());
            if (targetNode == null) {
                Node tmpNode = new Node(null, elem, null, null, null);
                tmpNode.setParentIDOfNoninsertedNode(elem.getParent().getOrder());
                getNodesWithoutParent().add(tmpNode);
            } else {
                setRecentNodeInTree(targetNode.insertNewNode(elem, null, null, null));
            }

            for (int i = 0; i < elem.getSubBoxNumber(); i++) {
                if (elem.getSubBox(i) instanceof ElementBox) {
                    createNodesTree((ElementBox) elem.getSubBox(i));
                } else if (elem.getSubBox(i) instanceof ReplacedBox) {
                    createNodesTree((ReplacedBox) elem.getSubBox(i));
                } else if (elem.getSubBox(i) instanceof TextBox) {
                    createNodesTree((TextBox) elem.getSubBox(i));
                }
            }
        }
    }

    /**
     * create tree of element as nodes tree, when current element is instance of ReplacedBox
     *
     * @param box - replaced box
     */
    private void createNodesTree(ReplacedBox box) {
        Box convertedBox = (Box) box;
        if (convertedBox.getParent() == null) {
            setRootNodeOfTree(new Node(null, null, null, box, null));
            setRecentNodeInTree(getRootNodeOfTree());
        } else {
            Node targetNode = findNodeToInsert(convertedBox.getParent().getOrder(), convertedBox.getOrder());
            if (targetNode == null) {
                Node tmpNode = new Node(null, null, null, box, null);
                tmpNode.setParentIDOfNoninsertedNode(convertedBox.getParent().getOrder());
                nodesWithoutParent.add(tmpNode);
            } else {
                setRecentNodeInTree(targetNode.insertNewNode(null, null, box, null));
            }
        }
    }

    /**
     * create tree of element as nodes tree, when current element is text
     *
     * @param text - element representing text
     */
    private void createNodesTree(TextBox text) {
        if (text.getParent() == null) {
            setRootNodeOfTree(new Node(null, null, text, null, null));
            setRecentNodeInTree(getRootNodeOfTree());
        } else {
            Node targetNode = findNodeToInsert(text.getParent().getOrder(), text.getOrder());
            if (targetNode == null) {
                Node tmpNode = new Node(null, null, text, null, null);
                tmpNode.setParentIDOfNoninsertedNode(text.getParent().getOrder());
                nodesWithoutParent.add(tmpNode);
            } else {
                setRecentNodeInTree(targetNode.insertNewNode(null, text, null, null));
            }
        }
    }


    /**
     * FINISH STEP B - process the nodesWithoutParent table and insert nodes to
     * TREE, if possible
     */
    public void tryToInsertNotInsertedNodes() {
        // repeats until the table is empty
        while (nodesWithoutParent.size() > 0) {
            int sizeBefore = nodesWithoutParent.size();
            // goes through table and tries to find at least one record to add
            // to TREE
            for (int i = 0; i < nodesWithoutParent.size(); i++) {
                Node findMyParent = nodesWithoutParent.get(i);

                Node nodeToInsert = findNodeToInsert(findMyParent.getParentIDOfNoninsertedNode(), findMyParent.getID());
                // inserts the node, if parent node found in the tree
                if (nodeToInsert != null) {
                    nodeToInsert.insertNewNode(findMyParent);
                    nodesWithoutParent.remove(i);
                }
            }
            // if non of the records can not bee added to the TREE, it breaks
            // the cycle
            if (sizeBefore == nodesWithoutParent.size()) break;
        }
    }


    ////////////////////////////////////////////////////////////////////////
    // CREATE BREAK/AVOID TABLES
    //
    ////////////////////////////////////////////////////////////////////////

    /**
     * Goes throw TREE and inserts items into breakTable and into avoidTable
     */
    public void createBreakAvoidTables() {
        List<Node> queueOpen = new ArrayList<>(16);
        queueOpen.add(rootNodeOfTree);

        // goes through TREE
        while (queueOpen.size() > 0) {
            Node recNodeToInvestigate = queueOpen.get(0);
            queueOpen.remove(0);

            if (recNodeToInvestigate.isElem()) {
                // gets CSS property for further classification
                NodeData style = recNodeToInvestigate.getElem().getStyle();
                CSSProperty.PageBreak pgbefore = style.getProperty("page-break-before");
                CSSProperty.PageBreak pgafter = style.getProperty("page-break-after");
                CSSProperty.PageBreakInside pginside = style.getProperty("page-break-inside");

                // element contains page-break-before: always; CSS property
                if (pgbefore != null && pgbefore == CSSProperty.PageBreak.ALWAYS) {
                    // creates empty record
                    float[] tableRec = new float[4];

                    // finds start of the interval
                    Node temp = getElementAbove(recNodeToInvestigate);
                    if (temp == null)
                        tableRec[0] = recNodeToInvestigate.getParentNode().getElemY()
                                + recNodeToInvestigate.getParentNode().getPlusOffset();
                    else
                        tableRec[0] = getLastBottom(temp);

                    // finds ends of the interval
                    tableRec[1] = getFirstTop(recNodeToInvestigate);

                    tableRec[2] = recNodeToInvestigate.getElemY();

                    //if end of page is above actual element, is not necessary make new page before actual element
                    // inserts into breakTable
                    insertIntoTable(tableRec, breakTable);

                }

                // element contains page-break-after: always; CSS property
                if (pgafter != null && pgafter == CSSProperty.PageBreak.ALWAYS) {
                    // creates empty record
                    float[] tableRec = new float[4];

                    // finds start of the interval
                    tableRec[0] = getLastBottom(recNodeToInvestigate);

                    // finds ends of the interval
                    Node temp = getElementBelow(recNodeToInvestigate);
                    if (temp != null) {
                        tableRec[1] = getFirstTop(temp);
                    } else {
                        tableRec[1] = recNodeToInvestigate.getElemY()
                                + recNodeToInvestigate.getElemHeight();
                    }

                    // finds the break place
                    tableRec[2] = recNodeToInvestigate.getElemY()
                            + recNodeToInvestigate.getElemHeight();

                    // inserts into breakTable
                    insertIntoTable(tableRec, breakTable);
                }

                // element contains page-break-before: avoid; CSS property
                if (pgbefore != null && pgbefore == CSSProperty.PageBreak.AVOID) {
                    // creates empty record
                    float[] tableRec = new float[4];

                    // finds start of the interval
                    Node temp = getElementAbove(recNodeToInvestigate);
                    if (temp != null) {
                        tableRec[0] = getLastBottom(temp);
                    } else {
                        tableRec[0] = recNodeToInvestigate.getElemY();
                    }

                    // finds ends of the interval
                    tableRec[1] = getFirstTop(recNodeToInvestigate);

                    // finds the break place
                    tableRec[2] = temp.getElemY();

                    // inserts into avoidTable
                    insertIntoTable(tableRec, avoidTable);
                }

                // element contains page-break-after: avoid; CSS property
                if (pgafter != null && pgafter == CSSProperty.PageBreak.AVOID) {
                    // creates empty record
                    float[] tableRec = new float[4];

                    // finds start of the interval
                    tableRec[0] = getLastBottom(recNodeToInvestigate);

                    // finds ends of the interval
                    Node temp = getElementBelow(recNodeToInvestigate);
                    if (temp != null) {
                        tableRec[1] = getFirstTop(temp);
                    } else {
                        tableRec[1] = recNodeToInvestigate.getElemY()
                                + recNodeToInvestigate.getElemHeight();
                    }

                    // finds the break place
                    tableRec[2] = recNodeToInvestigate.getElemY();
                    // inserts into avoidTable
                    insertIntoTable(tableRec, avoidTable);
                }

                // element contains page-break-inside: avoid; CSS property
                if (pginside != null && pginside == CSSProperty.PageBreakInside.AVOID) {
                    // creates empty record
                    float[] tableRec = new float[4];

                    // finds start of the interval
                    tableRec[0] = recNodeToInvestigate.getElem().getAbsoluteBorderBounds().y - 1;

                    // finds ends of the interval
                    tableRec[1] = tableRec[0] + recNodeToInvestigate.getElem().getAbsoluteBorderBounds().getHeight() + 1;

                    // finds the break place
                    tableRec[2] = tableRec[0] - 1;

                    // inserts into avoidTable
                    insertIntoTable(tableRec, avoidTable);
                }
            }

            // adds all children to the end of queueOpen
            if (recNodeToInvestigate.getAllChildren() != null) {

                Vector<Node> test = recNodeToInvestigate.getAllChildren();
                queueOpen.addAll(recNodeToInvestigate.getAllChildren());
            }
        }
    }

    /**
     * Inserts record into breakTable or into avoidTable
     */
    private void insertIntoTable(float[] tableRec, List<float[]> table) {

        boolean inserted = false;
        for (int i = 0; i < table.size(); i++) {
            if (tableRec[0] < table.get(i)[0]) {
                table.add(i, tableRec);
                inserted = true;
                break;
            }
        }
        if (!inserted)
            table.add(tableRec);
    }

    /**
     * Deletes items in Avoid table that are higher than "biggerThan" of the
     * page height
     */
    public void deleteAvoidsBiggerThan(float biggerThan, PDFOutput pdf) {
        for (int i = 0; i < avoidTable.size(); i++) {
            if (avoidTable.get(i)[1] - avoidTable.get(i)[0] > biggerThan * pdf.getPageHeight())
                avoidTable.remove(i);
        }
    }


    /**
     * Merges avoid interval that are overlapping
     */
    public void mergeAvoids(float biggerThan, PDFOutput pdf) {
        // goes through table
        for (int i = 1; i < avoidTable.size(); i++) {
            // tests if intervals in records are overlapping
            if (avoidTable.get(i - 1)[1] > avoidTable.get(i)[0]) {
                // tests size of interval if it is not larger than allowed
                if (avoidTable.get(i)[1] - avoidTable.get(i - 1)[0] > biggerThan * pdf.getPageHeight()) {
                    avoidTable.remove(i);
                    i--;
                }
                // merges overlapping records
                else {
                    if (avoidTable.get(i - 1)[1] < avoidTable.get(i)[1])
                        avoidTable.get(i - 1)[1] = avoidTable.get(i)[1];
                    avoidTable.remove(i);
                    i--;
                }
            }
        }
    }

    /**
     * Updates all tables by moving all break/avoid lines
     *
     * @param moveBy - offset for element displacement
     */
    private void updateTables(float moveBy) {
        // moves all records in breakTable
        for (int i = 0; i < breakTable.size(); i++) {
            breakTable.get(i)[0] += moveBy;
            breakTable.get(i)[1] += moveBy;
            breakTable.get(i)[2] += moveBy;
        }
        // moves all records in avoidTable
        for (int i = 0; i < avoidTable.size(); i++) {
            avoidTable.get(i)[0] += moveBy;
            avoidTable.get(i)[1] += moveBy;
            avoidTable.get(i)[2] += moveBy;
        }
    }


    /////////////////////////////////////////////////////////////////////
    // FUNCTIONS FOR MAKING PAGING
    //
    /////////////////////////////////////////////////////////////////////

    /**
     * Finds the element above element
     *
     * @param recentNode - current element
     * @return the Node
     */
    private Node getElementAbove(Node recentNode) {
        if (recentNode == null)
            return null;

        Node nParent = recentNode.getParentNode();
        if (nParent == null)
            return null;

        List<Node> nChildren = nParent.getAllChildren();
        if (nChildren == null)
            return null;

        Node nodeX = null;
        // goes through whole TREE
        while (nChildren.size() > 0) {
            Node temp = nChildren.get(0);
            nChildren.remove(0);

            // if recent child's ID is equal to original nod's ID - continue
            if (recentNode.getID() == temp.getID())
                continue;

            // if the child is not above - continue
            if (temp.getElemY() + temp.getPlusOffset() + temp.getElemHeight()
                    + temp.getPlusHeight() > recentNode.getElemY() + recentNode.getPlusOffset())
                continue;

            if (nodeX == null)
                nodeX = temp;
            else if (nodeX.getElemY() + nodeX.getPlusOffset() + nodeX.getElemHeight()
                    + nodeX.getPlusHeight() <= temp.getElemY() + temp.getPlusOffset()
                    + temp.getElemHeight() + temp.getPlusHeight()) {
                nodeX = temp;
            }
        }
        return nodeX;
    }

    /**
     * Finds the element below element
     *
     * @param recentNode - current element
     * @return the Node
     */
    private Node getElementBelow(Node recentNode) {
        if (recentNode == null)
            return null;
        // gets Vector of all parents children (including the node itself)
        Node nParent = recentNode.getParentNode();
        if (nParent == null)
            return null;
        List<Node> nChildren = nParent.getAllChildren();

        if (nChildren == null)
            return null;
        Node wantedNode = null;

        // goes through all children and search for node below the node given
        while (nChildren.size() > 0) {
            // gets first element from Vector
            Node temp = nChildren.get(0);
            nChildren.remove(0);

            // continues if recent node is the same as the original node
            if (recentNode.getID() == temp.getID())
                continue;

            // new candidate is not under recent node
            if (temp.getElemY() + temp.getPlusOffset() < recentNode.getElemY()
                    + recentNode.getElemHeight() + recentNode.getPlusHeight() + recentNode.getPlusOffset()) {
                continue;
            }

            // wantedNode gets new reference if it has not one yet or the old
            // node
            // contains element with lower position then new candidate
            if (wantedNode == null)
                wantedNode = temp;
            else if (wantedNode.getElemY() + wantedNode.getPlusOffset() >= temp.getElemY()
                    + temp.getPlusOffset()) {
                wantedNode = temp;
            }
        }
        return wantedNode;
    }

    /**
     * Finds the top of first child element in Node
     *
     * @param recentNode - current element
     * @return the resized distance from top of the document or -1 for not null
     * argument
     */
    private float getFirstTop(Node recentNode) {
        if (recentNode == null)
            return -1;

        List<Node> nChildren = recentNode.getAllChildren();
        if (nChildren == null)
            return recentNode.getElemY() + recentNode.getPlusOffset();

        float vysledekNeelem = Float.MAX_VALUE;
        float vysledekElem = Float.MAX_VALUE;

        // goes through subTREE and searches for first not-ElementBox element
        // - in case it doesn't contain any not-ElementBox element, it would
        // pick first ElementBox element
        List<Node> subTree = nChildren;

        while (subTree.size() > 0) {
            Node aktualni = subTree.get(0);
            subTree.remove(0);
            List<Node> subChildren = aktualni.getAllChildren();
            if (subChildren != null)
                subTree.addAll(subChildren);

            if (aktualni.isElem()) {
                if (aktualni.getElemY() + aktualni.getPlusOffset() < vysledekElem) {
                    vysledekElem = aktualni.getElemY() + aktualni.getPlusOffset();
                }
            } else {
                if (aktualni.getElemY() + aktualni.getPlusOffset() < vysledekNeelem) {
                    vysledekNeelem = aktualni.getElemY() + aktualni.getPlusOffset();
                }
            }
        }
        if (vysledekNeelem != Float.MAX_VALUE)
            return vysledekNeelem;
        if (vysledekElem != Float.MAX_VALUE)
            return vysledekElem;

        return -2;
    }

    /**
     * Finds the bottom of last child element in Node
     *
     * @param recentNode - current element
     * @return the resized distance from top of the document
     */
    private float getLastBottom(Node recentNode) {

        if (recentNode == null) return -1;
        List<Node> nChildren = recentNode.getAllChildren();
        float a = recentNode.getElemY();
        float b = recentNode.getElemHeight();
        if (nChildren == null)
            return recentNode.getElemY() + recentNode.getElemHeight()
                    + recentNode.getPlusOffset() + recentNode.getPlusHeight();

        float vysledekNeelem = recentNode.getElemY() + recentNode.getElemHeight();
        float vysledekElem = recentNode.getElemY() + recentNode.getElemHeight();

        // goes through subTREE and searches for last not-ElementBox element
        // - in case it doesn't contain any not-ElementBox element, it would
        // pick last ElementBox element
        List<Node> subTree = nChildren;

        while (subTree.size() > 0) {
            Node aktualni = subTree.get(0);
            subTree.remove(0);
            List<Node> subChildren = aktualni.getAllChildren();
            if (subChildren != null) subTree.addAll(subChildren);

            if (aktualni.isElem()) {
                if (aktualni.getElemY() + aktualni.getElemHeight() + aktualni.getPlusOffset()
                        + aktualni.getPlusHeight() > vysledekElem)
                    vysledekElem = aktualni.getElemY() + aktualni.getElemHeight()
                            + aktualni.getPlusOffset() + aktualni.getPlusHeight();
            } else {
                if (aktualni.getElemY() + aktualni.getElemHeight() + aktualni.getPlusOffset()
                        + aktualni.getPlusHeight() > vysledekNeelem)
                    vysledekNeelem = aktualni.getElemY() + aktualni.getElemHeight()
                            + aktualni.getPlusOffset() + aktualni.getPlusHeight();
            }
        }

        if (vysledekNeelem != -Float.MAX_VALUE)
            return vysledekNeelem;
        if (vysledekElem != -Float.MAX_VALUE)
            return vysledekElem;

        return -2;
    }

    ////////////////////////////////////////////////////////////////////////
    // MAKE BREAK ON LINE1, LINE2 OR LINE3 AND ADD PLUSOFFSET OR PLUSPADDING FOR ALL ELEM BELLOW ACTUAL ELEMENT
    //
    ////////////////////////////////////////////////////////////////////////

    /**
     * Makes end of page by moving elements in TREE according to line1
     *
     * @param line1 - line for presumed page break
     */
    private void makeBreakAt(float line1, PDFOutput pdf) {
//        if (line1 > pdfRenderer.getRootHeight() || line1 < 0)
//            return;

        float spaceBetweenLines = 0;

        line1 -= 0;

        // goes through TREE end finds set of all non-ElementbBox elements which
        // are crossed by the line1
        // - picks one element from this set, which has the lowest distance from
        // the top of the page
        List<Node> myOpen = new ArrayList<>(2);
        myOpen.add(rootNodeOfTree);

        float line2 = line1;
        while (myOpen.size() > 0) {
            Node myRecentNode = myOpen.get(0);
            myOpen.remove(0);
            List<Node> myChildren = myRecentNode.getAllChildren();
            if (myChildren != null) {
                myOpen.addAll(myChildren);
            }

            float startOfTheElement = myRecentNode.getElemY() + myRecentNode.getPlusOffset();
            float endOfTheElement = startOfTheElement + myRecentNode.getElemHeight()
                    + myRecentNode.getPlusHeight();

            // sets the line2 variable to match the top of the element from set
            // which has the lowest distance from the top of the document
            if (!myRecentNode.isElem() || myRecentNode.getElem().isReplaced()) {
                if (startOfTheElement < line1 && endOfTheElement > line1) {
                    if (startOfTheElement < line2) {
                        line2 = startOfTheElement;
                    }
                }
            }
        }

        // counts line3
        List<Node> myOpen2 = new ArrayList<>(2);
        myOpen2.add(rootNodeOfTree);

        float line3 = line2;
        while (myOpen2.size() > 0) {
            Node myRecentNode2 = myOpen2.get(0);
            myOpen2.remove(0);
            List<Node> myChildren2 = myRecentNode2.getAllChildren();
            if (myChildren2 != null) {
                myOpen2.addAll(myChildren2);
            }

            float startOfTheElement = myRecentNode2.getElemY() + myRecentNode2.getPlusOffset();
            float endOfTheElement = startOfTheElement + myRecentNode2.getElemHeight()
                    + myRecentNode2.getPlusHeight();

            // counts the line3
            if (!myRecentNode2.isElem() || myRecentNode2.getElem().isReplaced()) {
                if (startOfTheElement < line2 && endOfTheElement > line2) {
                    if (startOfTheElement < line3) {
                        line3 = startOfTheElement;
                    }
                }
            }
        }

        // counts distance between lines
        float a = pdf.getPageHeight();
        double b = (line1 - 1) / pdf.getPageHeight();
        float c = line3;
        spaceBetweenLines = (float) (pdf.getPageHeight() * Math.ceil((line1 - 1) / pdf.getPageHeight()) - line3);

        // goes through TREE and increases height or moves element
        List<Node> myOpen3 = new ArrayList<>(2);
        myOpen3.add(rootNodeOfTree);

        while (myOpen3.size() > 0) {
            Node myRecentNode = myOpen3.get(0);
            myOpen3.remove(0);
            List<Node> myChildren = myRecentNode.getAllChildren();
            if (myChildren != null) {
                myOpen3.addAll(myChildren);
            }

            // counts start and end of the element
            float startOfTheElement = myRecentNode.getElemY() + myRecentNode.getPlusOffset();
            float endOfTheElement = startOfTheElement + myRecentNode.getElemHeight()
                    + myRecentNode.getPlusHeight();

            // whole element if above the line2 - nothing happens
            if (endOfTheElement <= line2) {
            }
            // increases the height of element which:
            // - is ElementBox
            // - is crossed by the line2
            // - has got at least 2 children
            else if (myRecentNode.isElem() && myRecentNode.getElemY() + myRecentNode.getPlusOffset() < line2
                    && myRecentNode.getElemY() + myRecentNode.getPlusOffset()
                    + myRecentNode.getElemHeight() + myRecentNode.getPlusHeight() >= line2
                    && myRecentNode.getAllChildren() != null) {
                myRecentNode.addPlusHeight(spaceBetweenLines);
            }
            // moves element in one of following cases:
            // - element is completely below the line2
            // - element is crossing line2 and is not ElementBox
            else {
                myRecentNode.addPlusOffset(spaceBetweenLines);
            }
        }

        // updates height of the original document
        float rootHeight = pdf.getRootHeight() + spaceBetweenLines;
        pdf.setRootHeight(rootHeight);
        // updates values in all records in avoidTable and breakTable
        updateTables(spaceBetweenLines);
    }


    ////////////////////////////////////////////////////////////////////////
    // ENDS OF PAGES
    //
    ////////////////////////////////////////////////////////////////////////

    /**
     * Control if element is on start of page
     *
     * @param breakTable - break table
     */
    private boolean elemIsOnStartOfPage(List<float[]> breakTable) {
        int pages1 = 0;
        int pages2 = 0;
        float pageSize1 = breakTable.get(0)[0];
        float pageSize2 = breakTable.get(0)[2];
        while (pageSize1 > 0.0f) {
            pageSize1 -= pdfRenderer.getPdf().getPageHeight();
            pages1++;
        }

        while (pageSize2 > 0.0f) {
            pageSize2 -= pdfRenderer.getPdf().getPageHeight();
            pages2++;
        }

        if (pages1 == pages2) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * STEP D - makes paging in the TREE data structure according to data in
     * breakTable, avoidTable and the ends determined by the size of document
     * page
     */
    public void makePaging(PDFOutput pdf) {
        float pageEnd = pdf.getPageHeight();
        while (breakTable.size() > 0 || pageEnd < pdfRenderer.getRootHeight()) {
            if (breakTable.size() == 0 || pageEnd < breakTable.get(0)[0]) {
                boolean nalezeno = false;
                for (int i = 0; i < avoidTable.size(); i++) {
                    if (avoidTable.get(i)[0] < pageEnd && avoidTable.get(i)[1] > pageEnd) {
                        makeBreakAt(avoidTable.get(i)[2], pdf);
                        // sets new end of page according to height of the page
                        // in PDF document
                        pageEnd += pdf.getPageHeight();
                        nalezeno = true;
                    }
                }

                // not founded in avoidTable -> break normal
                if (!nalezeno) {
                    makeBreakAt(pageEnd, pdf);
                    // sets new end of page according to height of the page in
                    // PDF document
                    pageEnd += pdf.getPageHeight();
                }
            } else if (pageEnd > breakTable.get(0)[0] && pageEnd < breakTable.get(0)[1]) {
                if (breakTable.get(0)[2] > pageEnd) {

                    makeBreakAt(pageEnd, pdf);
                    // sets new end of page according to height of the page in
                    // PDF document
                    pageEnd += pdf.getPageHeight();
                } else if (!elemIsOnStartOfPage(breakTable)) {
                    makeBreakAt(breakTable.get(0)[2], pdf);
                }
                breakTable.remove(0);
            }
            // EOP is after the interval in first record of breakTable
            else {
                if (!elemIsOnStartOfPage(breakTable)) {
                    makeBreakAt(breakTable.get(0)[2], pdf);
                }
                breakTable.remove(0);
                pageEnd += pdf.getPageHeight();
            }
        }
    }

    /**
     * Find node which corresponding with current element
     *
     * @param elem - the element
     * @param node - current node
     * @return return node which corresponding with element
     */
    public Node getNodeByElement(ElementBox elem, Node node) {
        if (elem == node.getElem()) {
            return node;
        }
        Vector<Node> allChildren = node.getAllChildren();
        Node res = null;
        if (allChildren != null) {
            for (int i = 0; i < allChildren.size(); i++) {
                res = getNodeByElement(elem, allChildren.get(i));
                if (res != null) {
                    return res;
                }
            }
        }
        return res;
    }

    /**
     * Find node which corresponding with current text
     *
     * @param text - element representing text
     * @param node - current node
     * @return return node which corresponding with text
     */
    public Node getNodeByText(TextBox text, Node node) {
        if (text == node.getText()) {
            return node;
        }
        Vector<Node> allChildren = node.getAllChildren();
        Node res = null;
        if (allChildren != null) {
            for (int i = 0; i < allChildren.size(); i++) {
                res = getNodeByText(text, allChildren.get(i));
                if (res != null) {
                    return res;
                }
            }
        }
        return res;
    }

}
