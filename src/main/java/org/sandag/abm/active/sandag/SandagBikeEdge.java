package org.sandag.abm.active.sandag;
import org.sandag.abm.active.*;

public class SandagBikeEdge implements Edge<SandagBikeNode>
{
    private SandagBikeNode from, to;
    
    public byte bikeClass=0, lanes=0, functionalClass=0;
    public boolean centroidConnector=false, autosPermitted=false;
    public float distance=0;
    public short gain=0;
    
    public SandagBikeEdge(SandagBikeNode fromNode, SandagBikeNode toNode)
    {
        this.from = fromNode;
        this.to = toNode;
    }
    
    public SandagBikeNode getFromNode()
    {
        return from;
    }

    public SandagBikeNode getToNode()
    {
        return to;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((from == null) ? 0 : from.hashCode());
        result = prime * result + ((to == null) ? 0 : to.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        SandagBikeEdge other = (SandagBikeEdge) obj;
        if (from == null)
        {
            if (other.from != null) return false;
        } else if (!from.equals(other.from)) return false;
        if (to == null)
        {
            if (other.to != null) return false;
        } else if (!to.equals(other.to)) return false;
        return true;
    }
    
    
}
