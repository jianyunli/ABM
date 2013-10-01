package org.sandag.abm.active.sandag;
import org.sandag.abm.active.*;

public class SandagBikeTraversal implements Traversal<SandagBikeEdge>
{
    private SandagBikeEdge from, to;
    
    public TurnType turnType = TurnType.NONE;
    
    public SandagBikeTraversal(SandagBikeEdge fromEdge, SandagBikeEdge toEdge)
    {
        this.from = fromEdge;
        this.to = toEdge;
    }

    public SandagBikeEdge getFromEdge()
    {
        return from;
    }

    public SandagBikeEdge getToEdge()
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
        SandagBikeTraversal other = (SandagBikeTraversal) obj;
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
