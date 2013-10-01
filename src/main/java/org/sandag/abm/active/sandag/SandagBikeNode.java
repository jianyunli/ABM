package org.sandag.abm.active.sandag;
import org.sandag.abm.active.SimpleNode;

public class SandagBikeNode extends SimpleNode
{

    public float x=0 ,y=0;
    public short mgra=0 ,taz=0 ;
    public boolean signalized=false , centroid=false;
    
    public SandagBikeNode(int id)
    {
        super(id);
    }

}
