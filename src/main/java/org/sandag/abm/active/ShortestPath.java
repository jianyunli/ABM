package org.sandag.abm.active;

import java.util.Set;

public interface ShortestPath<N extends Node> {
	ShortestPathResults<N> getShortestPaths(Set<N> originNodes, Set<N> destinationNodes, double maxCost);
	ShortestPathResults<N> getShortestPaths(Set<N> originNodes, Set<N> destinationNodes);
}
