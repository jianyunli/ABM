package org.sandag.abm.active;
import java.util.*;

public class Network <N extends Node, E extends Edge<N>, T extends Traversal<E>>
{
   
    private Map<N,ArrayList<E>> succeedingEdges;
    private Map<N,ArrayList<E>> preceedingEdges;
    
    private Map<E,ArrayList<T>> succeedingTraversals;
    private Map<E,ArrayList<T>> preceedingTraversals;
    
    public Network()
    {
        succeedingEdges =  new LinkedHashMap<N,ArrayList<E>>();
        preceedingEdges =  new LinkedHashMap<N,ArrayList<E>>();
        succeedingTraversals =  new LinkedHashMap<E,ArrayList<T>>();
        preceedingTraversals =  new LinkedHashMap<E,ArrayList<T>>();
    }
    
    public E getEdge(N fromNode, N toNode)
    {
        for (E e : succeedingEdges.get(fromNode)) {
            if ( e.getToNode() == toNode ) { return e; }
        }
        throw new RuntimeException("fromNode and toNode do not form an edge");
    }
    
    public T getTraversal(E fromEdge, E toEdge)
    {
        for (T t : succeedingTraversals.get(fromEdge)) {
            if ( t.getToEdge() == toEdge ) { return t; }
        }
        throw new RuntimeException("fromEdge and toEdge do not form a traversal");
    }

    public Iterator<N> nodeIterator()
    {
        return succeedingEdges.keySet().iterator();
    }    
    
    private class SuccessorIterator implements Iterator<N>
    {

        Iterator<E> edgeIterator;
        
        public SuccessorIterator(N node)
        {
            edgeIterator = succeedingEdges.get(node).iterator();
        }

        public boolean hasNext()
        {
            return edgeIterator.hasNext();
        }

        public N next()
        {
            return (N) edgeIterator.next().getToNode();
        }

        public void remove()
        {
            throw new UnsupportedOperationException();
        }
 
    }
    
    private class PredecessorIterator implements Iterator<N>
    {

        Iterator<E> edgeIterator;
        
        public PredecessorIterator(N node)
        {
            edgeIterator = preceedingEdges.get(node).iterator();
        }

        public boolean hasNext()
        {
            return edgeIterator.hasNext();
        }

        public N next()
        {
            return (N) edgeIterator.next().getFromNode();
        }

        public void remove()
        {
            throw new UnsupportedOperationException();
        }
 
    }
    
    public Iterator<N> successorIterator(N node)
    {
        return new SuccessorIterator(node);
    }
    
    public Iterator<N> predecessorIterator(N node)
    {
        return new PredecessorIterator(node);
    }
    
    public List<N> getSuccessors(N node) {
        List<N> nodes = new ArrayList<N>();
        Iterator<N> it = successorIterator(node);
        while ( it.hasNext() ){
            nodes.add(it.next());
        }
        return nodes;
    }
    
    public List<N> getPredecessors(N node) {
        List<N> nodes = new ArrayList<N>();
        Iterator<N> it = predecessorIterator(node);
        while ( it.hasNext() ){
            nodes.add(it.next());
        }
        return nodes;
    }
 
    public Iterator<E> edgeIterator()
    {
        return succeedingTraversals.keySet().iterator();
    }

    public class TraversalIterator implements Iterator<T>
    {
        Iterator<E> edgeIterator;
        Iterator<T> succeedingTraversalIterator;
        
        TraversalIterator()
        {
            edgeIterator = edgeIterator();
            if  ( edgeIterator.hasNext() ) {
                succeedingTraversalIterator = succeedingTraversals.get(edgeIterator.next()).iterator();
            }
        }

        public boolean hasNext()
        {
            return ( succeedingTraversalIterator.hasNext() || edgeIterator.hasNext() );
        }

        public T next()
        {
            if ( ! succeedingTraversalIterator.hasNext() ) { 
                succeedingTraversalIterator = succeedingTraversals.get(edgeIterator.next()).iterator();
            }
            return succeedingTraversalIterator.next();
        }

        public void remove()
        {
            edgeIterator.remove();
        }
           
    }
    
    public Iterator<T> traversalIterator()
    {
        return new TraversalIterator();
    }
    

    public void addNode(N node)
    {
        if ( succeedingEdges.containsKey(node) ) {
            throw new IllegalStateException("Network already contains Node" + node.toString());
        }
        succeedingEdges.put(node, new ArrayList<E>());
        preceedingEdges.put(node, new ArrayList<E>());
    }
    
    public void addEdge(E edge)
    {
        N fromNode = (N) edge.getFromNode();
        N toNode = (N) edge.getToNode();
        
        if ( succeedingTraversals.containsKey(edge) ) {
            throw new IllegalStateException("Network already contains Edge" + edge.toString());
        } else {
        
            if ( ! succeedingEdges.containsKey(fromNode) ) { succeedingEdges.put(fromNode, new ArrayList<E>()); }
            if ( ! preceedingEdges.containsKey(toNode) ) { preceedingEdges.put(toNode, new ArrayList<E>()); }
            succeedingEdges.get(fromNode).add(edge);
            preceedingEdges.get(toNode).add(edge);
        }
        
        succeedingTraversals.put(edge, new ArrayList<T>());
        preceedingTraversals.put(edge, new ArrayList<T>());
      
    }
    
    public void addTraversal(T traversal)
    {
        E fromEdge = (E) traversal.getFromEdge();
        E toEdge = (E) traversal.getToEdge();
        
        if ( ! succeedingTraversals.containsKey(fromEdge) ) { succeedingTraversals.put(fromEdge, new ArrayList<T>()); }
        if ( ! preceedingTraversals.containsKey(toEdge) ) { succeedingTraversals.put(toEdge, new ArrayList<T>()); }
        
        if ( succeedingTraversals.get(fromEdge).contains(traversal) ) {
            throw new IllegalStateException("Network already contains Traversal" + traversal.toString());
        } else {
            succeedingTraversals.get(fromEdge).add(traversal);
            preceedingTraversals.get(toEdge).add(traversal);
        }
    }
    
    public boolean containsNode(N node) {
        return succeedingEdges.containsKey(node);
    }
    
    public boolean containsEdge(E edge) {
        N fromNode = (N) edge.getFromNode();
        return succeedingEdges.get(fromNode).contains(edge);
    }
    
    public boolean containsEdgeWithNodes(N fromNode, N toNode) {
        for (E edge : succeedingEdges.get(fromNode) ) {
            if ( toNode.equals(edge.getToNode()) ) {
                return true;
            }
        }
        return false;
    }
    
    public boolean containsTraversal(T traversal) {
        E fromEdge = (E) traversal.getFromEdge();
        return succeedingTraversals.get(fromEdge).contains(traversal);
    }
    
    public boolean containsTraversalWithEdges(E fromEdge, E toEdge) {
        for (T traversal : succeedingTraversals.get(fromEdge) ) {
            if ( toEdge.equals(traversal.getToEdge()) ) {
                return true;
            }
        }
        return false;
    }
        
}
