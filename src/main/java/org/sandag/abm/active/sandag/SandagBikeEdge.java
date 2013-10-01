package org.sandag.abm.active.sandag;
import org.sandag.abm.active.*;

public class SandagBikeEdge extends SimpleEdge<SandagBikeNode>
{

    public byte bikeClass=0, lanes=0, functionalClass=0;
    public boolean centroidConnector=false, autosPermitted=false;
    public float distance=0;
    public short gain=0;
    
    public SandagBikeEdge(SandagBikeNode fromNode, SandagBikeNode toNode)
    {
        super(fromNode, toNode);
    }
    
}
