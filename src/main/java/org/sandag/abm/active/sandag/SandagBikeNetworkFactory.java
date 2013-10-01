package org.sandag.abm.active.sandag;
import org.sandag.abm.active.*;
import java.util.*;
import java.io.*;
import java.lang.reflect.*;
import org.apache.log4j.Logger;
import com.linuxense.javadbf.*;

public class SandagBikeNetworkFactory extends NetworkFactory<SandagBikeNode,SandagBikeEdge,SandagBikeTraversal>
{
    private Map<String,String> propertyMap;
    private PropertyParser propertyParser;
    Map<Integer,SandagBikeNode> nodeIndex;
    
    private static final String PROPERTIES_NODE_FILE = "active.node.file";
    private static final String PROPERTIES_NODE_ID = "active.node.id";
    private static final String PROPERTIES_NODE_FIELDNAMES = "active.node.fieldnames";
    private static final String PROPERTIES_NODE_COLUMNS = "active.node.columns";
    private static final String PROPERTIES_EDGE_FILE = "active.edge.file";
    private static final String PROPERTIES_EDGE_ANODE = "active.edge.anode";
    private static final String PROPERTIES_EDGE_BNODE = "active.edge.bnode";
    private static final String PROPERTIES_EDGE_DIRECTIONAL = "active.edge.directional";
    private static final String PROPERTIES_EDGE_FIELDNAMES = "active.edge.fieldnames";
    private static final String PROPERTIES_EDGE_COLUMNS_AB = "active.edge.columns.ab";
    private static final String PROPERTIES_EDGE_COLUMNS_BA = "active.edge.columns.ba";
    private static final String PROPERTIES_EDGE_CENTROID_FIELD = "active.edge.centroid.field";
    private static final String PROPERTIES_EDGE_CENTROID_VALUE = "active.edge.centroid.value";
    private static final String PROPERTIES_EDGE_AUTOSPERMITTED_FIELD = "active.edge.autospermitted.field";
    private static final String PROPERTIES_EDGE_AUTOSPERMITTED_VALUES = "active.edge.autospermitted.values";
    
    private static final double TURN_ANGLE_TOLERANCE = Math.PI / 6;
    
    public SandagBikeNetworkFactory(Map<String, String> propertyMap)
    {
        super();
        this.propertyMap = propertyMap;
        propertyParser = new PropertyParser(propertyMap);
        logger = Logger.getLogger(SandagBikeNetworkFactory.class);
        nodeIndex = new HashMap<Integer,SandagBikeNode>();
    }
    
    protected void readNodes()
    {
        try{
            InputStream stream = new FileInputStream(propertyMap.get(PROPERTIES_NODE_FILE));
            DBFReader reader = new DBFReader(stream);
            Map<String,String> fieldMap = propertyParser.mapStringPropertyListToStrings(PROPERTIES_NODE_FIELDNAMES, PROPERTIES_NODE_COLUMNS);
            Field f;
            SandagBikeNode node;
            int fieldCount = reader.getFieldCount();
            Map<String,Integer> labels = new HashMap<String,Integer>();
            for (int i=0; i<fieldCount; i++) {
                labels.put(reader.getField(i).getName(), i);
            }
            Object[] rowObjects;
            while ( ( rowObjects = reader.nextRecord() ) != null )  {             
                int id = ((Number) rowObjects[labels.get(propertyMap.get(PROPERTIES_NODE_ID))] ).intValue();
                node = new SandagBikeNode(id);
                for (String fieldName : fieldMap.keySet()) {
                    try {
                        f = SandagBikeNode.class.getField(fieldName);
                        setNumericFieldWithCast(node, f, (Number) rowObjects[labels.get(fieldMap.get(fieldName))]);
                     } catch (NoSuchFieldException | SecurityException e) {
                        logger.error( "Exception caught getting class field " + fieldName + " for object of class " + node.getClass().getName(), e);
                        throw new RuntimeException();
                     }
                }
                network.addNode(node);
                nodeIndex.put(id, node);   
            }
        } catch  (IOException e) {
            logger.error( "Exception caught reading nodes from disk.", e);
            throw new RuntimeException();
        }    
    }
    
    protected SandagBikeTraversal createTraversalFromEdges(SandagBikeEdge fromEdge, SandagBikeEdge toEdge)
    {
        return new SandagBikeTraversal(fromEdge, toEdge);
    }
    
    protected void readEdges()
    {
        try {
            InputStream stream = new FileInputStream(propertyMap.get(PROPERTIES_EDGE_FILE));
            DBFReader reader = new DBFReader(stream);
            Map<String,String> abFieldMap = propertyParser.mapStringPropertyListToStrings(PROPERTIES_EDGE_FIELDNAMES, PROPERTIES_EDGE_COLUMNS_AB);
            Map<String,String> baFieldMap = new HashMap<String,String>();
            boolean directional = Boolean.parseBoolean(propertyMap.get(PROPERTIES_EDGE_DIRECTIONAL));
            if ( ! directional ) { baFieldMap = propertyParser.mapStringPropertyListToStrings(PROPERTIES_EDGE_FIELDNAMES, PROPERTIES_EDGE_COLUMNS_BA); }
            Field f;
            SandagBikeEdge edge;
            int columnCount = reader.getFieldCount();
            Map<String,Integer> labels = new HashMap<String,Integer>();
            for (int i=0; i<columnCount; i++) {
                labels.put(reader.getField(i).getName(), i);
            }
            Object[] rowObjects;
            while ( ( rowObjects = reader.nextRecord() ) != null )  {             
                SandagBikeNode aNode = nodeIndex.get( ( (Number) rowObjects[labels.get(propertyMap.get(PROPERTIES_EDGE_ANODE))] ).intValue() );
                SandagBikeNode bNode = nodeIndex.get( ( (Number) rowObjects[labels.get(propertyMap.get(PROPERTIES_EDGE_BNODE))] ).intValue() );
                
                edge = new SandagBikeEdge(aNode, bNode);
                for (String fieldName : abFieldMap.keySet()) {
                    try {
                        f = SandagBikeEdge.class.getField(fieldName);
                        setNumericFieldWithCast(edge, f, (Number) rowObjects[labels.get(abFieldMap.get(fieldName))]);
                    } catch (NoSuchFieldException | SecurityException e) {
                        logger.error( "Exception caught getting class field " + fieldName + " for object of class " + edge.getClass().getName(), e);
                        throw new RuntimeException();
                    }
                }
                addEdgeWithConstructedTraversals(edge);
                
                if ( ! directional ){
                    edge = new SandagBikeEdge(bNode, aNode);
                    for (String fieldName : baFieldMap.keySet()) {
                        try {
                            f = SandagBikeEdge.class.getField(fieldName);
                            setNumericFieldWithCast(edge, f, (Number) rowObjects[labels.get(baFieldMap.get(fieldName))]);
                        } catch (NoSuchFieldException | SecurityException e) {
                            logger.error( "Exception caught getting class field " + fieldName + " for object of class " + edge.getClass().getName(), e);
                            throw new RuntimeException();
                        }
                    }            
                    addEdgeWithConstructedTraversals(edge);
                }                
            }
        } catch  (IOException e) {
            logger.error( "Exception caught reading edges from disk.", e);
            throw new RuntimeException();
        }
    }
    
    protected void calculateDerivedNodeAttributes()
    {
        Iterator<SandagBikeNode> nodeIterator = network.nodeIterator();
        SandagBikeNode n;
        while ( nodeIterator.hasNext() ) {
            n = nodeIterator.next();
            n.centroid = ( n.mgra > 0 ) || ( n.taz > 0 );
        }
    }
    
    protected void calculateDerivedEdgeAttributes()
    {
        try {
            Iterator<SandagBikeEdge> edgeIterator;
            Field f;
            
            edgeIterator = network.edgeIterator();
            f = SandagBikeEdge.class.getField(propertyMap.get(PROPERTIES_EDGE_AUTOSPERMITTED_FIELD));
            SandagBikeEdge edge;
            while ( edgeIterator.hasNext() ) {
                edge = edgeIterator.next();
                edge.autosPermitted = propertyParser.isIntValueInPropertyList(f.getInt(edge),PROPERTIES_EDGE_AUTOSPERMITTED_VALUES);
            }
            
            edgeIterator = network.edgeIterator();
            f = SandagBikeEdge.class.getField(propertyMap.get(PROPERTIES_EDGE_CENTROID_FIELD));
            while ( edgeIterator.hasNext() ) {
                edge = edgeIterator.next();
                edge.centroidConnector = propertyParser.isIntValueInPropertyList(f.getInt(edge),PROPERTIES_EDGE_CENTROID_VALUE);
            }
            
        } catch (NoSuchFieldException | IllegalAccessException e) {
            logger.error( "Exception caught calculating derived edge attributes.", e);
            throw new RuntimeException();
        }
    }
    
    protected void calculateDerivedTraversalAttributes()
    {     
        Iterator<SandagBikeTraversal> traversalIterator = network.traversalIterator();
        SandagBikeTraversal t;
        while ( traversalIterator.hasNext() ) {
            t = traversalIterator.next();
            t.turnType = calculateTurnType(t);
        }
    }
    
    private double calculateTraversalAngle(SandagBikeTraversal t)
    {    
        float xDiff1 = (t.getFromEdge().getToNode()).x - (t.getFromEdge().getFromNode()).x;
        float xDiff2 = (t.getToEdge().getToNode()).x - (t.getToEdge().getFromNode()).x;
        float yDiff1 = (t.getFromEdge().getToNode()).y - (t.getFromEdge().getFromNode()).y;
        float yDiff2 = (t.getToEdge().getToNode()).y - (t.getToEdge().getFromNode()).y;
        
        double angle = Math.atan2(yDiff2, xDiff2) - Math.atan2(yDiff1, xDiff1);
        
        if ( angle > Math.PI ) {
            angle = angle - 2 * Math.PI;
        }
        if (angle < - Math.PI ) {
            angle = angle + 2 * Math.PI;
        }
    
        return angle;
    }
    
    private TurnType calculateTurnType(SandagBikeTraversal t)
    {        
        SandagBikeNode start = t.getFromEdge().getFromNode();
        SandagBikeNode thru = t.getFromEdge().getToNode();
        SandagBikeNode end = t.getToEdge().getToNode();
        
        if ( (start).centroid || (thru).centroid || (end).centroid ) {
            return TurnType.NONE;
        }
        
        if ( start.equals(end) ) {
            return TurnType.REVERSAL;
        }
        
        double thisAngle = calculateTraversalAngle(t);
    
        if ( thisAngle < - Math.PI + TURN_ANGLE_TOLERANCE || thisAngle > Math.PI - TURN_ANGLE_TOLERANCE ) {
            return TurnType.REVERSAL;
        }
        
        double minAngle = Math.PI;
        double maxAngle = -Math.PI;
        double minAbsAngle = Math.PI;
        double currentAngle;
        int legCount = 1;
        
        for (SandagBikeNode successor : network.getSuccessors(thru) ) {
            SandagBikeEdge edge = network.getEdge(thru, successor);
            if ( edge.autosPermitted && ! successor.equals(start) ){
                currentAngle = calculateTraversalAngle(network.getTraversal(t.getFromEdge(),edge));
                minAngle = Math.min(minAngle, currentAngle);
                maxAngle = Math.max(maxAngle, currentAngle);
                minAbsAngle = Math.min(minAbsAngle, Math.abs(currentAngle));
                legCount += 1;
            }
        }
        
        if ( legCount <= 2 ) {
            return TurnType.NONE;
        } else if ( legCount == 3) {
            if ( thisAngle <= minAngle && Math.abs(thisAngle) > TURN_ANGLE_TOLERANCE ) {
                return TurnType.RIGHT;
            } else if ( thisAngle >= maxAngle && Math.abs(thisAngle) > TURN_ANGLE_TOLERANCE ) {
                return TurnType.LEFT;
            } else {
                return TurnType.NONE;
            }
        } else {
            if ( Math.abs(thisAngle) <= minAbsAngle || ( Math.abs(thisAngle) < TURN_ANGLE_TOLERANCE && thisAngle > minAngle && thisAngle < maxAngle ) ) {
                return TurnType.NONE;
            } else if ( thisAngle < 0 ) {
                return TurnType.RIGHT;
            } else {
                return TurnType.LEFT;            
            }
        }
    }
        
    private void setNumericFieldWithCast(Object o, Field f, Number n) {
        Class<?> c = f.getType();
        try {
            if ( c.equals(Integer.class) || c.equals(Integer.TYPE) ) {
                f.set(o,n.intValue());      
            } else if ( c.equals(Float.class) || c.equals(Float.TYPE) ) {
                f.set(o,n.floatValue());
            } else if ( c.equals(Double.class) || c.equals(Double.TYPE) ) {
                f.set(o,n.doubleValue());
            } else if ( c.equals(Boolean.class) || c.equals(Boolean.TYPE) ) {
                f.set(o, n.intValue() == 1);
            } else if ( c.equals(Byte.class) || c.equals(Byte.TYPE)) {
                f.set(o, n.byteValue());
            } else if ( c.equals(Short.class) || c.equals(Short.TYPE)) {
                f.set(o, n.shortValue());
            } else if ( c.equals(Long.class) || c.equals(Long.TYPE)) {
                f.set(o, n.longValue());
            } else {
                throw new RuntimeException("Field " + f.getName() + " in class " + o.getClass().getName() + " is not numeric");
            }
        } catch (IllegalArgumentException | IllegalAccessException e) {
            logger.error( "Exception caught setting class field " + f.getName() + " for object of class " + o.getClass().getName(), e);
            throw new RuntimeException();
        }
    }
}
