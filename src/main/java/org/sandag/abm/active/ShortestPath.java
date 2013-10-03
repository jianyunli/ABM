package org.sandag.abm.active;

import java.util.Map;
import java.util.Set;

public interface ShortestPath<N extends Node> {
	ShortestPathResults<N> getShortestPaths(Set<N> originNodes, Set<N> destinationNodes, double maxCost);
	ShortestPathResults<N> getShortestPaths(Set<N> originNodes, Set<N> destinationNodes);
	ShortestPathResults<N> getShortestPaths(Map<N,Set<N>> originsDestinations, double maxCost);
	ShortestPathResults<N> getShortestPaths(Map<N,Set<N>> originsDestinations);
}
