package org.sandag.abm.active;

import org.apache.log4j.Logger;

public abstract class NetworkFactory<N extends Node, E extends Edge<N>, T extends Traversal<E>>
{
    protected Logger logger = Logger.getLogger(NetworkFactory.class);
    protected Network<N,E,T> network;
    
    protected NetworkFactory()
    {
        network = new Network<N,E,T>();
    }

    public Network<N,E,T> create()
    {
        readNodes();
        readEdges();
        calculateDerivedNodeAttributes();
        calculateDerivedEdgeAttributes();
        calculateDerivedTraversalAttributes();
        return network;
    }
    
    public void addEdgeWithConstructedTraversals(E edge)
    {
        network.addEdge(edge);
        
        N fromNode = (N) edge.getFromNode();
        N toNode = (N) edge.getToNode();
    
        for (N successor : network.getSuccessors(toNode)) {
            E succeedingEdge = network.getEdge(toNode,successor);
            if ( ! network.containsTraversalWithEdges(edge,succeedingEdge) ) {
                network.addTraversal(createTraversalFromEdges(edge,succeedingEdge));
            }
        }
        
        for (N predecessor : network.getPredecessors(fromNode)) {
            E preceedingEdge = network.getEdge(predecessor,fromNode);
            if ( ! network.containsTraversalWithEdges(preceedingEdge,edge) ) {
                network.addTraversal(createTraversalFromEdges(preceedingEdge, edge));
            }
        }
    }
    
    protected abstract void readNodes();
    protected abstract void readEdges();
    protected abstract T createTraversalFromEdges(E fromEdge, E toEdge);
    protected abstract void calculateDerivedNodeAttributes();
    protected abstract void calculateDerivedEdgeAttributes();
    protected abstract void calculateDerivedTraversalAttributes();
}
