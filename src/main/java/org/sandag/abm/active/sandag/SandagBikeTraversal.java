package org.sandag.abm.active.sandag;
import org.sandag.abm.active.*;

public class SandagBikeTraversal extends SimpleTraversal<SandagBikeEdge>
{
    
    public TurnType turnType = TurnType.NONE;
    
    public SandagBikeTraversal(SandagBikeEdge fromEdge, SandagBikeEdge toEdge)
    {
        super(fromEdge,toEdge);
    }

}
