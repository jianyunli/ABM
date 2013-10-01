package org.sandag.abm.active.sandag;
import org.sandag.abm.active.Node;

public class SandagBikeNode implements Node
{
    private int id;
    public float x=0 ,y=0;
    public short mgra=0 ,taz=0 ;
    public boolean signalized=false , centroid=false;
    
    public SandagBikeNode(int id)
    {
        this.id = id;
    }
    
    public int getId()
    {
        return id;
    }

    public void setId(int id)
    {
        this.id = id;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + id;
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        SandagBikeNode other = (SandagBikeNode) obj;
        if (id != other.id) return false;
        return true;
    }
    
    
}
