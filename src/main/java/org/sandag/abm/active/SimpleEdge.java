package org.sandag.abm.active;

import java.util.Objects;

public class SimpleEdge<N extends Node> implements Edge<N>
{
    private final N fromNode;
    private final N toNode;
    
    public SimpleEdge(N fromNode, N toNode) {
    	this.fromNode = fromNode;
    	this.toNode = toNode;
    }

    @Override
    public N getFromNode()
    {
        return fromNode;
    }

    @Override
    public N getToNode()
    {
        return toNode;
    }
    
    @Override
    public int hashCode() 
    {
    	return Objects.hash(fromNode,toNode);
    }
    
    @Override
    public boolean equals(Object o) 
    {
    	if ((o == null) || !(o instanceof Edge))
    		return false;
    	Edge<?> other = (Edge<?>) o;
    	return fromNode.equals(other.getFromNode()) && toNode.equals(other.getToNode());
    }
}
