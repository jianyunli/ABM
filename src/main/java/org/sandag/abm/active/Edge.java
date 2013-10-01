package org.sandag.abm.active;

public interface Edge<N extends Node>
{
    N getFromNode();
    N getToNode();
}
