package org.sandag.abm.active;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public abstract class AbstractShortestPath<N extends Node> implements ShortestPath<N> {

	@Override
	public ShortestPathResults<N> getShortestPaths(Set<N> originNodes, Set<N> destinationNodes, double maxCost) {
		Map<N,Set<N>> originsDestinations = new HashMap<>();
		for (N originNode : originNodes)
			originsDestinations.put(originNode,destinationNodes);
		return getShortestPaths(originsDestinations,maxCost);
	}

	@Override
	public ShortestPathResults<N> getShortestPaths(Set<N> originNodes, Set<N> destinationNodes) {
		return getShortestPaths(originNodes,destinationNodes,Double.POSITIVE_INFINITY);
	}

	@Override
	public ShortestPathResults<N> getShortestPaths(Map<N, Set<N>> originsDestinations) {
		return getShortestPaths(originsDestinations,Double.POSITIVE_INFINITY);
	}

}
