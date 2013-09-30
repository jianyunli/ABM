package org.sandag.abm.active;

import java.util.*;
import org.apache.log4j.Logger;

public abstract class NetworkFactory<T, U extends DirectionalPair, V extends DirectionalPair>
{
    protected Logger logger = Logger.getLogger(NetworkFactory.class);
    protected Network<T,U,V> network;
    
    protected NetworkFactory()
    {
        network = new Network<T,U,V>();
    }

    public Network<T,U,V> create()
    {
        readNodes();
        readEdges();
        calculateDerivedNodeAttributes();
        calculateDerivedEdgeAttributes();
        calculateDerivedTraversalAttributes();
        return network;
    }
    
    public void addEdgeWithConstructedTraversals(U edge)
    {
        network.addEdge(edge);
        
        T fromNode = (T) edge.getFrom();
        T toNode = (T) edge.getTo();
    
        for (T successor : network.getSuccessors(toNode)) {
            U succeedingEdge = network.getEdge(toNode,successor);
            if ( ! network.containsTraversalWithEdges(edge,succeedingEdge) ) {
                network.addTraversal(createTraversalFromEdges(edge,succeedingEdge));
            }
        }
        
        for (T predecessor : network.getPredecessors(fromNode)) {
            U preceedingEdge = network.getEdge(predecessor,fromNode);
            if ( ! network.containsTraversalWithEdges(preceedingEdge,edge) ) {
                network.addTraversal(createTraversalFromEdges(preceedingEdge, edge));
            }
        }
    }
    
    protected abstract void readNodes();
    protected abstract void readEdges();
    protected abstract V createTraversalFromEdges(U fromEdge, U toEdge);
    protected abstract void calculateDerivedNodeAttributes();
    protected abstract void calculateDerivedEdgeAttributes();
    protected abstract void calculateDerivedTraversalAttributes();
}
