package org.sandag.abm.active;

import java.util.HashMap;
import java.util.Map;

public class DijkstraOOCacheShortestPath<N extends Node,E extends Edge<N>,T extends Traversal<E>> extends DijkstraOOShortestPath<N,E,T> {
	private final Map<T,Double> cachedCosts;

	public DijkstraOOCacheShortestPath(Network<N,E,T> network,TraversalEvaluator<T> traversalEvaluator) {
		super(network,traversalEvaluator);
		cachedCosts = new HashMap<>();
	}
	
	protected double evaluateTraversal(T traversal) {
		Double value = cachedCosts.get(traversal);
		if (value == null) {
			value = super.evaluateTraversal(traversal);
			cachedCosts.put(traversal,value);
		}
		return value; 
	}

}
