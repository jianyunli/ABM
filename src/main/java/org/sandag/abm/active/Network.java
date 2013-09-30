package org.sandag.abm.active;
import java.util.*;

public class Network <T, U extends DirectionalPair, V extends DirectionalPair>
{
   
    private Map<T,ArrayList<U>> succeedingEdges;
    private Map<T,ArrayList<U>> preceedingEdges;
    
    private Map<U,ArrayList<V>> succeedingTraversals;
    private Map<U,ArrayList<V>> preceedingTraversals;
    
    public Network()
    {
        succeedingEdges =  new LinkedHashMap<T,ArrayList<U>>();
        preceedingEdges =  new LinkedHashMap<T,ArrayList<U>>();
        succeedingTraversals =  new LinkedHashMap<U,ArrayList<V>>();
        preceedingTraversals =  new LinkedHashMap<U,ArrayList<V>>();
    }
    
    public U getEdge(T fromNode, T toNode)
    {
        for (U e : succeedingEdges.get(fromNode)) {
            if ( e.getTo() == toNode ) { return e; }
        }
        throw new RuntimeException("fromNode and toNode do not form an edge");
    }
    
    public V getTraversal(U fromEdge, U toEdge)
    {
        for (V t : succeedingTraversals.get(fromEdge)) {
            if ( t.getTo() == toEdge ) { return t; }
        }
        throw new RuntimeException("fromEdge and toEdge do not form a traversal");
    }

    public Iterator<T> nodeIterator()
    {
        return succeedingEdges.keySet().iterator();
    }    
    
    private class SuccessorIterator implements Iterator<T>
    {

        Iterator<U> edgeIterator;
        
        public SuccessorIterator(T node)
        {
            edgeIterator = succeedingEdges.get(node).iterator();
        }

        public boolean hasNext()
        {
            return edgeIterator.hasNext();
        }

        public T next()
        {
            return (T) edgeIterator.next().getTo();
        }

        public void remove()
        {
            throw new UnsupportedOperationException();
        }
 
    }
    
    private class PredecessorIterator implements Iterator<T>
    {

        Iterator<U> edgeIterator;
        
        public PredecessorIterator(T node)
        {
            edgeIterator = preceedingEdges.get(node).iterator();
        }

        public boolean hasNext()
        {
            return edgeIterator.hasNext();
        }

        public T next()
        {
            return (T) edgeIterator.next().getFrom();
        }

        public void remove()
        {
            throw new UnsupportedOperationException();
        }
 
    }
    
    public Iterator<T> successorIterator(T node)
    {
        return new SuccessorIterator(node);
    }
    
    public Iterator<T> predecessorIterator(T node)
    {
        return new PredecessorIterator(node);
    }
    
    public List<T> getSuccessors(T node) {
        List<T> nodes = new ArrayList<T>();
        Iterator<T> it = successorIterator(node);
        while ( it.hasNext() ){
            nodes.add(it.next());
        }
        return nodes;
    }
    
    public List<T> getPredecessors(T node) {
        List<T> nodes = new ArrayList<T>();
        Iterator<T> it = predecessorIterator(node);
        while ( it.hasNext() ){
            nodes.add(it.next());
        }
        return nodes;
    }
 
    public Iterator<U> edgeIterator()
    {
        return succeedingTraversals.keySet().iterator();
    }

    public class TraversalIterator implements Iterator<V>
    {
        Iterator<U> edgeIterator;
        Iterator<V> succeedingTraversalIterator;
        
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

        public V next()
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
    
    public Iterator<V> traversalIterator()
    {
        return new TraversalIterator();
    }
    

    public void addNode(T node)
    {
        if ( succeedingEdges.containsKey(node) ) {
            throw new IllegalStateException("Network already contains Node" + node.toString());
        }
        succeedingEdges.put(node, new ArrayList<U>());
        preceedingEdges.put(node, new ArrayList<U>());
    }
    
    public void addEdge(U edge)
    {
        T fromNode = (T) edge.getFrom();
        T toNode = (T) edge.getTo();
        
        if ( succeedingTraversals.containsKey(edge) ) {
            throw new IllegalStateException("Network already contains Edge" + edge.toString());
        } else {
        
            if ( ! succeedingEdges.containsKey(fromNode) ) { succeedingEdges.put(fromNode, new ArrayList<U>()); }
            if ( ! preceedingEdges.containsKey(toNode) ) { preceedingEdges.put(toNode, new ArrayList<U>()); }
            succeedingEdges.get(fromNode).add(edge);
            preceedingEdges.get(toNode).add(edge);
        }
        
        succeedingTraversals.put(edge, new ArrayList<V>());
        preceedingTraversals.put(edge, new ArrayList<V>());
      
    }
    
    public void addTraversal(V traversal)
    {
        U fromEdge = (U) traversal.getFrom();
        U toEdge = (U) traversal.getTo();
        
        if ( ! succeedingTraversals.containsKey(fromEdge) ) { succeedingTraversals.put(fromEdge, new ArrayList<V>()); }
        if ( ! preceedingTraversals.containsKey(toEdge) ) { succeedingTraversals.put(toEdge, new ArrayList<V>()); }
        
        if ( succeedingTraversals.get(fromEdge).contains(traversal) ) {
            throw new IllegalStateException("Network already contains Traversal" + traversal.toString());
        } else {
            succeedingTraversals.get(fromEdge).add(traversal);
            preceedingTraversals.get(toEdge).add(traversal);
        }
    }
    
    public boolean containsNode(T node) {
        return succeedingEdges.containsKey(node);
    }
    
    public boolean containsEdge(U edge) {
        T fromNode = (T) edge.getFrom();
        return succeedingEdges.get(fromNode).contains(edge);
    }
    
    public boolean containsEdgeWithNodes(T fromNode, T toNode) {
        for (U edge : succeedingEdges.get(fromNode) ) {
            if ( toNode.equals(edge.getTo()) ) {
                return true;
            }
        }
        return false;
    }
    
    public boolean containsTraversal(V traversal) {
        U fromEdge = (U) traversal.getFrom();
        return succeedingTraversals.get(fromEdge).contains(traversal);
    }
    
    public boolean containsTraversalWithEdges(U fromEdge, U toEdge) {
        for (V traversal : succeedingTraversals.get(fromEdge) ) {
            if ( toEdge.equals(traversal.getTo()) ) {
                return true;
            }
        }
        return false;
    }
        
}
