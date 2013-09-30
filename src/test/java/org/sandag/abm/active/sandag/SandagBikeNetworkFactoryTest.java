package org.sandag.abm.active.sandag;
import java.util.*;
import org.sandag.abm.active.*;

import static org.junit.Assert.*;
import org.junit.*;

public class SandagBikeNetworkFactoryTest
{
    final static String RESOURCE_BUNDLE_NAME = "sandag_abm_active_test";
    Map<String,String> propertyMap = new HashMap<String,String>();
    SandagBikeNetworkFactory factory;
    Network<SandagBikeNode, SandagBikeEdge, SandagBikeTraversal> network;
    
    @Before
    public void setUp() {
        ResourceBundle rb = ResourceBundle.getBundle(RESOURCE_BUNDLE_NAME);
        propertyMap = new HashMap<String,String>();
        Enumeration<String> keys = rb.getKeys();
        while (keys.hasMoreElements()) {
            String key = keys.nextElement();
            propertyMap.put(key, rb.getString(key));
        }
        factory = new SandagBikeNetworkFactory(propertyMap);
        network = factory.create();
    }
    
    @Test
    public void testIteratorAndGetReturnSameEdge()
    {
        Iterator<SandagBikeEdge> edgeIterator = network.edgeIterator();
        SandagBikeEdge edge1, edge2;
        while (edgeIterator.hasNext()) {
            edge1 = edgeIterator.next();
            edge2 = network.getEdge(edge1.getFrom(), edge1.getTo());
            assertEquals(edge1, edge2);
        }
    }
    
    @Test
    public void testIteratorAndGetReturnSameTraversal()
    {
        Iterator<SandagBikeTraversal> traversalIterator = network.traversalIterator();
        SandagBikeTraversal traversal1, traversal2;
        while (traversalIterator.hasNext()) {
            traversal1 = traversalIterator.next();
            traversal2 = network.getTraversal(traversal1.getFrom(), traversal1.getTo());
            assertEquals(traversal1, traversal2);
        }
    }
    
    @Test
    public void testEachSuccessorFormsEdge()
    {
        Iterator<SandagBikeNode> nodeIterator = network.nodeIterator();
        SandagBikeNode node;
        while (nodeIterator.hasNext()) {
            node = nodeIterator.next();
            for (SandagBikeNode successor : network.getSuccessors(node) ) {
                assertTrue(network.containsEdgeWithNodes(node, successor));
            }
        }
    }
    
    @Test
    public void testEachPredecessorFormsEdge()
    {
        Iterator<SandagBikeNode> nodeIterator = network.nodeIterator();
        SandagBikeNode node;
        while (nodeIterator.hasNext()) {
            node = nodeIterator.next();
            for (SandagBikeNode predecessor : network.getPredecessors(node) ) {
                assertTrue(network.containsEdgeWithNodes(predecessor, node));
            }
        }
    }
    
    @Test
    public void testEachEdgeInSuccessorsOnce() {
        Iterator <SandagBikeEdge> edgeIterator = network.edgeIterator();
        SandagBikeEdge edge;
        while (edgeIterator.hasNext()) {
            edge = edgeIterator.next();
            int count = 0;
            for (SandagBikeNode node : network.getSuccessors(edge.getFrom()) ) {
                count = count + ( node.equals(edge.getTo()) ? 1 : 0);
            }
            assertEquals(1,count);
        }
    }
    
    @Test
    public void testEachEdgeInPredecessorsOnce() {
        Iterator <SandagBikeEdge> edgeIterator = network.edgeIterator();
        SandagBikeEdge edge;
        while (edgeIterator.hasNext()) {
            edge = edgeIterator.next();
            int count = 0;
            for (SandagBikeNode node : network.getPredecessors(edge.getTo()) ) {
                count = count + ( node.equals(edge.getFrom()) ? 1 : 0);
            }
            assertEquals(1,count);
        }
    }
    
    @Test
    public void testEachEdgeAndSuccessorFormsTraversal() {
        Iterator <SandagBikeEdge> edgeIterator = network.edgeIterator();
        SandagBikeEdge edge;
        while (edgeIterator.hasNext()) {
            edge = edgeIterator.next();
            for (SandagBikeNode s : network.getSuccessors(edge.getTo()) ) {
                assertTrue(network.containsTraversalWithEdges(edge,network.getEdge(edge.getTo(),s)));
            }
        }
    }
    
    @Test
    public void testEachEdgeAndPredecessorFormsTraversal() {
        Iterator <SandagBikeEdge> edgeIterator = network.edgeIterator();
        SandagBikeEdge edge;
        while (edgeIterator.hasNext()) {
            edge = edgeIterator.next();
            for (SandagBikeNode p : network.getPredecessors(edge.getFrom()) ) {
                assertTrue(network.containsTraversalWithEdges(network.getEdge(p,edge.getFrom()),edge));
            }
        }
    }
    
    @Test
    public void testFieldValuesMatchInput() {
        SandagBikeNode node = factory.nodeIndex.get(100003629);
        assertEquals(3629, node.mgra);
        assertEquals(6264311, node.x, 5);
        
        SandagBikeEdge edge = network.getEdge( factory.nodeIndex.get(755011),  factory.nodeIndex.get(753841));
        assertEquals(580.8311, edge.distance, 0.001);
        edge = network.getEdge( factory.nodeIndex.get(746401),  factory.nodeIndex.get(749381));
        assertEquals(5, edge.gain);
        edge = network.getEdge( factory.nodeIndex.get(749381),  factory.nodeIndex.get(746401));
        assertEquals(0, edge.gain);
    }
    
    @Test
    public void testTurnTypesMatchInput() {
        int start, thru, end;
        
        start = 728811; thru = 727491; end =728811;
        SandagBikeTraversal traversal = network.getTraversal( network.getEdge(factory.nodeIndex.get(start), factory.nodeIndex.get(thru)), network.getEdge(factory.nodeIndex.get(thru), factory.nodeIndex.get(end)));
        assertEquals(TurnType.REVERSAL, traversal.turnType);
        
        start = 728811; thru = 727491; end =723691;
        traversal = network.getTraversal( network.getEdge(factory.nodeIndex.get(start), factory.nodeIndex.get(thru)), network.getEdge(factory.nodeIndex.get(thru), factory.nodeIndex.get(end)));
        assertEquals(TurnType.NONE, traversal.turnType);
        
        start = 728811; thru = 727491; end =727871;
        traversal = network.getTraversal( network.getEdge(factory.nodeIndex.get(start), factory.nodeIndex.get(thru)), network.getEdge(factory.nodeIndex.get(thru), factory.nodeIndex.get(end)));
        assertEquals(TurnType.LEFT,traversal.turnType);
        
        start = 728811; thru = 727491; end =726911;
        traversal = network.getTraversal( network.getEdge(factory.nodeIndex.get(start), factory.nodeIndex.get(thru)), network.getEdge(factory.nodeIndex.get(thru), factory.nodeIndex.get(end)));
        assertEquals(TurnType.RIGHT, traversal.turnType);
        
        start = 723511; thru = 725251; end =723691;
        traversal = network.getTraversal( network.getEdge(factory.nodeIndex.get(start), factory.nodeIndex.get(thru)), network.getEdge(factory.nodeIndex.get(thru), factory.nodeIndex.get(end)));
        assertEquals(TurnType.RIGHT, traversal.turnType);
        
        start = 723511; thru = 725251; end =726911;
        traversal = network.getTraversal( network.getEdge(factory.nodeIndex.get(start), factory.nodeIndex.get(thru)), network.getEdge(factory.nodeIndex.get(thru), factory.nodeIndex.get(end)));        
        assertEquals(TurnType.NONE, traversal.turnType);
        
        start = 726911; thru = 723691; end =725251;
        traversal = network.getTraversal( network.getEdge(factory.nodeIndex.get(start), factory.nodeIndex.get(thru)), network.getEdge(factory.nodeIndex.get(thru), factory.nodeIndex.get(end)));
        assertEquals(TurnType.RIGHT, traversal.turnType);
        
        start = 739131; thru = 739421; end =736701;
        traversal = network.getTraversal( network.getEdge(factory.nodeIndex.get(start), factory.nodeIndex.get(thru)), network.getEdge(factory.nodeIndex.get(thru), factory.nodeIndex.get(end)));
        assertEquals(TurnType.NONE, traversal.turnType);
        
        start = 762181; thru = 760261; end =759961;
        traversal = network.getTraversal( network.getEdge(factory.nodeIndex.get(start), factory.nodeIndex.get(thru)), network.getEdge(factory.nodeIndex.get(thru), factory.nodeIndex.get(end)));
        assertEquals(TurnType.RIGHT, traversal.turnType);
        
        start = 743031; thru = 741901; end =100003897;
        traversal = network.getTraversal( network.getEdge(factory.nodeIndex.get(start), factory.nodeIndex.get(thru)), network.getEdge(factory.nodeIndex.get(thru), factory.nodeIndex.get(end)));
        assertEquals(TurnType.NONE, traversal.turnType);
        
        start = 743031; thru = 741901; end =740811;
        traversal = network.getTraversal( network.getEdge(factory.nodeIndex.get(start), factory.nodeIndex.get(thru)), network.getEdge(factory.nodeIndex.get(thru), factory.nodeIndex.get(end)));
        assertEquals(TurnType.NONE, traversal.turnType);
        
        start = 743031; thru = 741901; end =742821;
        traversal = network.getTraversal( network.getEdge(factory.nodeIndex.get(start), factory.nodeIndex.get(thru)), network.getEdge(factory.nodeIndex.get(thru), factory.nodeIndex.get(end)));
        assertEquals(TurnType.LEFT, traversal.turnType);
    }
}
