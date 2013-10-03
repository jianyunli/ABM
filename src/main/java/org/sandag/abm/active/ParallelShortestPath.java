package org.sandag.abm.active;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;

import com.pb.sawdust.util.concurrent.DnCRecursiveTask;

public class ParallelShortestPath<N extends Node> extends AbstractShortestPath<N> {
	private final ShortestPath<N> sp;
	private final ParallelMethod method;
	
	public ParallelShortestPath(ShortestPath<N> sp, ParallelMethod method) {
		this.sp = sp;
		this.method = method;
	}
	
	public static enum ParallelMethod {
		FORK_JOIN,
		QUEUE
	}


	@Override
	public ShortestPathResults<N> getShortestPaths(Map<N, Set<N>> originsDestinations, double maxCost) {
		switch (method) {
			case FORK_JOIN : {
				ShortestPathRecursiveTask task = new ShortestPathRecursiveTask(sp,originsDestinations,maxCost);
				new ForkJoinPool().execute(task);
				ShortestPathResultsContainer<N> sprc = task.getResult();
				return sprc;
			}
			case QUEUE : {
				int threadCount = Runtime.getRuntime().availableProcessors();
				ExecutorService executor = Executors.newFixedThreadPool(threadCount);
				final Queue<ShortestPathResultsContainer<N>> sprcQueue = new ConcurrentLinkedQueue<>();
				final Queue<N> originNodeQueue = new ConcurrentLinkedQueue<>(originsDestinations.keySet());
				ThreadLocal<ShortestPathResultsContainer<N>> sprcThreadLocal = new ThreadLocal<ShortestPathResultsContainer<N>>() {
					@Override
					public ShortestPathResultsContainer<N> initialValue() {
						ShortestPathResultsContainer<N> sprc = new BasicShortestPathResults<>();
						sprcQueue.add(sprc);
						return sprc;
					}
				};
				final CountDownLatch latch = new CountDownLatch(threadCount);
				final AtomicInteger counter = new AtomicInteger();
				for (int i = 0; i < threadCount; i++)
					executor.execute(new QueueMethodTask(sp,originNodeQueue,originsDestinations,maxCost,counter,sprcThreadLocal,latch));
				try {
					latch.await();
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
				executor.shutdown();
				
				ShortestPathResultsContainer<N> finalContainer = null;
				for (ShortestPathResultsContainer<N> sprc : sprcQueue)
					if (finalContainer == null)
						finalContainer = sprc;
					else
						finalContainer.addAll(sprc);
				
				return finalContainer;
			}
			default : throw new IllegalStateException("Should not be here.");
		}
	}

	@Override
	public ShortestPathResults<N> getShortestPaths(Set<N> originNodes, Set<N> destinationNodes) {
		return getShortestPaths(originNodes,destinationNodes,Double.POSITIVE_INFINITY);
	}
	
	private class QueueMethodTask implements Runnable {
		private final ShortestPath<N> sp;
		private final Queue<N> originNodes;
		private final Map<N,Set<N>> originsDestinations;
		private final double maxCost;
		private final AtomicInteger counter;
		private final ThreadLocal<ShortestPathResultsContainer<N>> spr;
		private final CountDownLatch latch;
		
		private QueueMethodTask(ShortestPath<N> sp, Queue<N> originNodes, Map<N,Set<N>> originsDestinations, double maxCost, AtomicInteger counter, ThreadLocal<ShortestPathResultsContainer<N>> spr, CountDownLatch latch) {
			this.sp = sp;
			this.originsDestinations = originsDestinations;
			this.originNodes = originNodes;
			this.maxCost = maxCost;
			this.counter = counter;
			this.spr = spr;
			this.latch = latch;
		}

		@Override
		public void run() {
			int segmentSize = 5;
			final Map<N,Set<N>> threadOriginDestinations = new HashMap<>();
			while (originNodes.size() > 0) {
				while ((originNodes.size() > 0) && (threadOriginDestinations.size() < segmentSize)) {
					N origin = originNodes.poll();
					if (origin != null)
						threadOriginDestinations.put(origin,originsDestinations.get(origin));
				}
				if (threadOriginDestinations.size() == 0)
					break;
				ShortestPathResults<N> result = sp.getShortestPaths(threadOriginDestinations,maxCost);
				ShortestPathResultsContainer<N> sprc = spr.get();
				for (ShortestPathResult<N> spResult : result.getResults()) 
					sprc.addResult(spResult);
				int c = counter.addAndGet(threadOriginDestinations.size()); 
				if (c % segmentSize < threadOriginDestinations.size())
					System.out.println("   done with " + ((c / segmentSize)*segmentSize) + " origins");
				threadOriginDestinations.clear();
			}
			latch.countDown();
		}
	}
	
	
	private class ShortestPathRecursiveTask extends DnCRecursiveTask<ShortestPathResultsContainer<N>> {
		AtomicInteger counter;
		private final ShortestPath<N> sp;
		private final Map<N,Set<N>> originsDestinations;
		private final Node[] origins;
		private final double maxCost;

		protected ShortestPathRecursiveTask(ShortestPath<N> sp, Map<N,Set<N>> originsDestinations, double maxCost) {
			super(0,originsDestinations.size());
			this.sp = sp;
			this.origins = originsDestinations.keySet().toArray(new Node[originsDestinations.size()]);
			this.originsDestinations = originsDestinations;
			this.maxCost = maxCost;
			counter = new AtomicInteger(0);
		}

		protected ShortestPathRecursiveTask(long start, long length, DnCRecursiveTask<ShortestPathResultsContainer<N>> next,
				                            ShortestPath<N> sp, Node[] origins, Map<N,Set<N>> originsDestinations, double maxCost, AtomicInteger counter) {
			super(start,length,next);
			this.sp = sp;
			this.origins = origins;
			this.originsDestinations = originsDestinations;
			this.maxCost = maxCost;
			this.counter = counter;
		}

		@Override
		@SuppressWarnings("unchecked") //origins only hold N, we just can't declare as such because of generics
		protected ShortestPathResultsContainer<N> computeTask(long start, long length) {
			Map<N,Set<N>> taskOriginsDestinations = new HashMap<>();
			int end = (int) (start + length);
			for (int n = (int) start; n < end; n++) {
				N originNode = (N) origins[n];
				taskOriginsDestinations.put(originNode,originsDestinations.get(originNode));
			}
			ShortestPathResults<N> result = sp.getShortestPaths(taskOriginsDestinations);
			ShortestPathResultsContainer<N> spr = new BasicShortestPathResults<>();
			for (ShortestPathResult<N> spResult : result.getResults()) 
				spr.addResult(spResult);
			
			int c = counter.addAndGet((int) length); 
			if (c % 10 < length)
				System.out.println("   done with " + ((c / 10)*10) + " origins");
			return spr;
		}

		@Override
		protected boolean continueDividing(long newLength) {
			return (newLength > 5) && (getSurplusQueuedTaskCount() < 3);
		}

		@Override
		protected DnCRecursiveTask<ShortestPathResultsContainer<N>> getNextTask(long start, long length, DnCRecursiveTask<ShortestPathResultsContainer<N>> next) {
			return new ShortestPathRecursiveTask(start,length,next,sp,origins,originsDestinations,maxCost,counter);
		}

		@Override
		protected ShortestPathResultsContainer<N> joinResults(ShortestPathResultsContainer<N> spr1, ShortestPathResultsContainer<N> spr2) {
			if (spr1.size() > spr2.size()) {
				spr1.addAll(spr2);
				return spr1;
			} else {
				spr2.addAll(spr1);
				return spr2;
			}
		}
	}
	
	

}
