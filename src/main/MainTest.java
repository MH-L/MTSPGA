package main;

import java.awt.Graphics2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.swing.JFrame;

import net.sf.javaml.clustering.KMeans;
import net.sf.javaml.core.Dataset;
import net.sf.javaml.core.DefaultDataset;
import net.sf.javaml.core.DenseInstance;
import net.sf.javaml.core.Instance;

public class MainTest {
	/**
	 * Parameters by problem definition
	 */
	private static int mapHeight = 2000;
	private static int mapWidth = 3000;
	private static int numDestinations = 100;
	private static int randomNumIterations = 2000; // Irrelevant
	private static int numCars = 5;
	private static int minTour = -1; // without any constraints
	private static int maxTour = -1; // without any constraints
	private static SimplePoint depotLocation;
	private static List<SimplePoint> destLocations;
	private static boolean useVariableCars = false;
	private static int carCost = 200;
	private static boolean randomDepot = false;
	private static boolean loadPointsFromFile = false;
	private static long initialMinCost = 0;
	private static RouteRendererFrame rf;
	private static RouteRenderer rr;
	
	/**
	 * Storage locations
	 */
	private static String baseDir = "C:\\Users\\bml\\Desktop\\Algorithms\\MTSPGA\\";
	private static String reportSuffix = "Report\\";
	private static String pointsSuffix = "Pointset\\";
	
	/**
	 * Tunable parameters for algorithm
	 */
	private static int populationSize = 100;
	private static int numGAIterations = 10000;
	private static int methodCount = 8;
	private static boolean useMultithreading = false;
	private static SelectionStrategy ss = SelectionStrategy.GROUP_AND_MAX;
	
	private enum SelectionStrategy {
		GROUP_AND_MAX,  // group population and select max of each group
		GROUP_AND_RANDOM,  // group population and select a random instance (better
						   // ones have greater chance to be chosen
		SORT_AND_SHUFFLE,  // sort population, shuffle and take the first ones
	}
	
	public static void main(String[] args) throws InterruptedException {
		MainTest mt = new MainTest();
		System.out.println(isIntersecting(mt.new SimplePoint(1, 5), mt.new SimplePoint(2, 4),
				mt.new SimplePoint(4, 4), mt.new SimplePoint(1, 1)));
		System.out.println(isIntersecting(mt.new SimplePoint(3, 1), mt.new SimplePoint(3, 2),
				mt.new SimplePoint(4, 4), mt.new SimplePoint(1, 1)));
		System.out.println(isIntersecting(mt.new SimplePoint(3, 1), mt.new SimplePoint(4, 6),
				mt.new SimplePoint(8, 8), mt.new SimplePoint(1, 1)));
		rr = new RouteRenderer();
		rf = new RouteRendererFrame(rr);
		rf.setVisible(true);
		Configuration cfg = Configuration.getInstance();
		loadConfiguration(cfg);
		long systemCurrentTime = System.currentTimeMillis();
		mt.doCalc(randomNumIterations);
		System.out.println("Total running time is: " + (System.currentTimeMillis() - systemCurrentTime));
	}
	
	public static void setDepotLocation(SimplePoint sp) {
		depotLocation = sp;
	}
	
	private static void loadConfiguration(Configuration cfg) {
		populationSize = (int) (cfg.getValue("population_size") == null ? populationSize : cfg.getValue("population_size"));
		numGAIterations = (int) (cfg.getValue("GA_iteration") == null ? numGAIterations : cfg.getValue("GA_iteration"));
		numDestinations = (int) (cfg.getValue("num_destinations") == null ? numDestinations : cfg.getValue("num_destinations"));
		randomDepot = cfg.getValue("random_depot") == null ? randomDepot : ((int) cfg.getValue("random_depot") == 1);
		numCars = (int) (cfg.getValue("num_cars") == null ? numCars : cfg.getValue("num_cars"));
		useMultithreading = cfg.getValue("use_multithreading") == null ? useMultithreading : (int) cfg.getValue("use_multithreading") == 1;
		baseDir = (String) (cfg.getValue("base_dir_location") == null ? baseDir : cfg.getValue("base_dir_location"));
		reportSuffix = (String) (cfg.getValue("report_suffix") == null ? reportSuffix : cfg.getValue("report_suffix"));
		pointsSuffix = (String) (cfg.getValue("points_suffix") == null ? pointsSuffix : cfg.getValue("points_suffix"));
		mapHeight = (int) (cfg.getValue("map_height") == null ? mapHeight : cfg.getValue("map_height"));
		mapWidth = (int) (cfg.getValue("map_width") == null ? mapWidth : cfg.getValue("map_width"));
		useVariableCars = cfg.getValue("use_variable_cars") == null ? useVariableCars : (int) cfg.getValue("use_variable_cars") == 1;
		carCost = (int) (cfg.getValue("single_car_cost") == null ? carCost : cfg.getValue("single_car_cost"));
		loadPointsFromFile = cfg.getValue("load_points_from_file") == null ? loadPointsFromFile : (int) cfg.getValue("load_points_from_file") == 1;
		minTour = (int) (cfg.getValue("min_tour") == null ? minTour : cfg.getValue("min_tour"));
		maxTour = (int) (cfg.getValue("max_tour") == null ? maxTour : cfg.getValue("max_tour"));
	}
	
	public void doCalc(int numIterations) throws InterruptedException {
		populateLocations();
		Map<Integer, List<SimplePoint>> partitions = partitionPoints(destLocations, depotLocation, 1000);

		List<List<SimplePoint>> clusterOfPoints = kmeansCluster(destLocations, 
				(int) Math.ceil((double) destLocations.size() / (double) 15));

		for (Integer groupKey : partitions.keySet()) {
			System.out.println(String.format("Group Number: %s, Size: %s", groupKey, partitions.get(groupKey).size()));
		}
		RoutePlan rp = new RoutePlan();
		List<Integer> optimalBreaks = new ArrayList<Integer>();
		long minCost = 1000000000000000000L;
		long grandTotalCost = 0;
		for (int i = 0; i < numIterations; i++) {
			rp.load();
			if (rp.getTotalCost() < minCost) {
				minCost = rp.getTotalCost();
				optimalBreaks.clear();
				for (int breakInteger : rp.breaks) {
					optimalBreaks.add(breakInteger);
				}
			}

			grandTotalCost += rp.getTotalCost();
		}
		
		System.out.println(String.format("The minimum cost is: %d", minCost));
		System.out.println(String.format("The average cost is: %d", grandTotalCost / numIterations));
		initialMinCost = minCost;
		printRoutes(rp);
		
		List<RoutePlan> optimalPlansForPartitions = new ArrayList<RoutePlan>();
		System.out.println("In total, we have " + partitions.keySet().size() + " partitions.");
//		for (Integer key : partitions.keySet()) {
//			List<SimplePoint> singlePart = partitions.get(key);
//			RoutePlan optPlan = doGA(populationSize, numGAIterations, (int) Math.ceil(singlePart.size() / 15), singlePart, false);
//			optimalPlansForPartitions.add(optPlan);
//		}
		
		// K-Means performances
		List<RoutePlan> optimalPlansKMeans = new ArrayList<RoutePlan>();
		for (List<SimplePoint> llst : clusterOfPoints) {
			RoutePlan optPlan = doGA(populationSize, 1000, (int) Math.ceil(llst.size() / 15), llst, false);
			optimalPlansKMeans.add(optPlan);
		}
	
		int sumForKMeans = 0;
		for (RoutePlan rPlan : optimalPlansKMeans) {
			sumForKMeans += rPlan.getTotalCost();
		}
		
		int sumForPlans = 0;
		for (RoutePlan rPlan : optimalPlansForPartitions) {
			sumForPlans += rPlan.getTotalCost();
		}

		RoutePlan optPlan = doGA(populationSize, 1000, (int) Math.ceil((double) destLocations.size() / (double) 15), 
				destLocations, true);
//		for (int breaksLen = 0; breaksLen < optPlan.breaks.size(); breaksLen++) {
//			System.out.println("Breaks value is: " + optPlan.breaks.get(breaksLen));
//		}
		System.out.println("Initial min cost is: " + initialMinCost);
		System.out.println("Partitioned min cost is: " + sumForPlans);
		System.out.println("KMeans min cost is: " + sumForKMeans);
	}
	
	private List<List<SimplePoint>> kmeansCluster(List<SimplePoint> pts, int numClusters) {
		Dataset pointsSet = new DefaultDataset();
		List<List<SimplePoint>> clusterOfPoints = new ArrayList<>();
		
		for (SimplePoint pt : pts) {
			Instance curInstance = new DenseInstance(new double[] {pt.xpos, pt.ypos});
			pointsSet.add(curInstance);
		}
		
		KMeans clusterer = new KMeans(numClusters);
		Dataset[] pointsClusters = clusterer.cluster(pointsSet);
		for (Dataset ds : pointsClusters) {
			List<SimplePoint> lst = new ArrayList<SimplePoint>();
			for (Instance ins : ds) {
				lst.add(new SimplePoint((int) ins.value(0), (int) ins.value(1)));
			}
			clusterOfPoints.add(lst);
		}
		return clusterOfPoints;
	}
	
	public static void resolveCrossings(List<SimplePoint> routeToBreak, SimplePoint depot, int startIndex, int endIndex) {
		
		if (endIndex - startIndex < 3)
			return;
		boolean hasNoCrossing = true;
		do {
			hasNoCrossing = true;
			outerloop:
			for (int i = startIndex + 1; i < endIndex; i++) {
				for (int j = startIndex - 1; j <= i - 2; j++) {
					SimplePoint pt1 = routeToBreak.get(i);
					SimplePoint pt11 = i + 1 >= endIndex ? depot : routeToBreak.get(i + 1);
					SimplePoint pt2 = j < startIndex ? depot : routeToBreak.get(j);
					SimplePoint pt22 = routeToBreak.get(j + 1);
					if (pt11.equals(depot) || pt2.equals(depot))
						continue;
					if (isIntersecting(pt1, pt11, pt2, pt22)) {
						System.out.println("Intersected!!");
						System.out.println(String.format("Point1: %s, %s", pt1.xpos, pt1.ypos));
						System.out.println(String.format("Point11: %s, %s", pt11.xpos, pt11.ypos));
						System.out.println(String.format("Point2: %s, %s", pt2.xpos, pt2.ypos));
						System.out.println(String.format("Point22: %s, %s", pt22.xpos, pt22.ypos));
						hasNoCrossing = false;
						if (i + 1 >= endIndex) {
							System.out.println("i + 1 is depot");
							routeToBreak.set(j, pt1);
							routeToBreak.set(i, pt2);
						} else if (j < startIndex) {
							System.out.println("J is depot");
							routeToBreak.set(i + 1, pt22);
							routeToBreak.set(j + 1, pt11);
						} else {
							SimplePoint temp = pt1;
							routeToBreak.set(i, pt22);
							routeToBreak.set(j + 1, temp);
						}
						
						rr.paintAll(new RoutePlan(routeToBreak, new ArrayList<Integer>(), depot), (Graphics2D) rr.getGraphics());
//						try {
//							Thread.sleep(3000);
//						} catch (InterruptedException e) {
//							// TODO Auto-generated catch block
//							e.printStackTrace();
//						}
						break outerloop;
					} else {
						System.out.println("Not Intersected!!");
						System.out.println(String.format("Point1: %s, %s", pt1.xpos, pt1.ypos));
						System.out.println(String.format("Point11: %s, %s", pt11.xpos, pt11.ypos));
						System.out.println(String.format("Point2: %s, %s", pt2.xpos, pt2.ypos));
						System.out.println(String.format("Point22: %s, %s", pt22.xpos, pt22.ypos));
					}
				}
			}
		} while (!hasNoCrossing);
	}
	
	public static boolean isIntersecting(SimplePoint pt1, SimplePoint pt11, SimplePoint pt2, SimplePoint pt22) {
		// y1 = k1*x1 + b1
		// y2 = k2*x2 + b2
		Double slope1 = (pt1.xpos == pt11.xpos) ? null : (double) (pt1.ypos - pt11.ypos) / (double) (pt1.xpos - pt11.xpos);
		Double firstIntercept = slope1 == null ? null : ((double) pt1.ypos - ((double) pt1.xpos) * slope1);
		Double slope2 = (pt2.xpos == pt22.xpos) ? null : (double) (pt2.ypos - pt22.ypos) / (double) (pt2.xpos - pt22.xpos);
		Double secondIntercept = slope2 == null ? null : ((double) pt2.ypos - ((double) pt2.xpos) * slope2);
		if (slope1 == null && slope2 == null)
			return false;
		
		if (slope1 != null && slope2 != null) {
			if (slope1.equals(slope2))
				return false;
			double x = (firstIntercept - secondIntercept) / (slope2 - slope1);
	    	return ((x >= pt2.xpos && x <= pt22.xpos) || (x <= pt2.xpos && x >= pt22.xpos))
					&& ((x >= pt1.xpos && x <= pt11.xpos) || (x <= pt1.xpos && x >= pt11.xpos));
		}
		
		if (slope1 == null) {
			// x1 == x11
			double x = pt1.xpos;
			double y = secondIntercept + slope2 * x;
			return ((x >= pt2.xpos && x <= pt22.xpos) || (x <= pt2.xpos && x >= pt22.xpos))
					&& ((y >= pt1.ypos && y <= pt11.ypos) || (y <= pt1.ypos && y >= pt11.ypos));
		}
		
		if (slope2 == null) {
			// x2 == x22
			double x = pt2.xpos;
			double y = firstIntercept + slope1 * x;
			return ((x >= pt1.xpos) && (x <= pt11.xpos) || (x <= pt1.xpos && x >= pt11.xpos))
					&& ((y >= pt2.ypos && y <= pt22.ypos) || (y <= pt2.ypos && y >= pt22.ypos));
		}
		
		return false;
//		Double slope1;
//		if (pt1.xpos == pt11.xpos)
//			slope1 = null;
//		else
//			slope1 = (double) (pt1.ypos - pt11.ypos) / (double) (pt1.xpos - pt11.xpos);
//		Double slope2;
//		if (pt2.xpos == pt22.xpos)
//			slope2 = null;
//		else
//			slope2 = (double) (pt2.ypos - pt22.ypos) / (double) (pt2.xpos - pt22.xpos);
//		
//		if (slope1 == slope2)
//			return false;
//		
//		if (slope1 == null) {
//			double intersectionY = pt2.ypos + (pt1.xpos - pt2.xpos) * slope2;
//			if (pt2.ypos > pt22.ypos)
//				return intersectionY >= pt22.ypos &&
//						((intersectionY >= pt1.ypos && intersectionY <= pt11.ypos) || 
//						(intersectionY <= pt1.ypos && intersectionY >= pt11.ypos));
//			else if (pt2.ypos == pt22.ypos)
//				return (pt1.xpos >= pt2.xpos && pt1.xpos <= pt22.xpos) ||
//						(pt1.xpos <= pt2.xpos && pt1.xpos >= pt22.xpos);
//			else
//				return intersectionY <= pt22.ypos &&
//						((intersectionY >= pt1.ypos && intersectionY <= pt11.ypos) || 
//						(intersectionY <= pt1.ypos && intersectionY >= pt11.ypos));
//		}
//		
//		if (slope2 == null) {
//			double intersectionY = pt1.ypos + (pt2.xpos - pt1.xpos) * slope1;
//			if (pt1.ypos > pt11.ypos)
//				return intersectionY >= pt11.ypos &&
//						((intersectionY >= pt2.ypos && intersectionY <= pt22.ypos) || 
//						(intersectionY <= pt2.ypos && intersectionY >= pt22.ypos));
//			else if (pt1.ypos == pt11.ypos)
//				return (pt2.xpos >= pt1.xpos && pt2.xpos <= pt11.xpos) ||
//						(pt2.xpos <= pt1.xpos && pt2.xpos >= pt11.xpos);
//			else
//				return intersectionY <= pt11.ypos &&
//					((intersectionY >= pt2.ypos && intersectionY <= pt22.ypos) || 
//					(intersectionY <= pt2.ypos && intersectionY >= pt22.ypos));
//		}
//		
//		double Y = (double) (pt1.ypos - pt2.ypos + slope1 * (pt2.xpos - pt1.xpos)) / (slope2 - slope1);
//		double X = Y + pt2.xpos - pt1.xpos;
//		System.out.println(Y);
//		System.out.println(X);
//		
//		return (pt1.xpos > pt11.xpos ? (X + pt1.xpos) >= pt11.xpos : (X + pt1.xpos) <= pt11.xpos)
//				&& (pt2.xpos > pt22.xpos ? (Y + pt2.xpos) >= pt22.xpos : (Y + pt2.xpos) <= pt22.xpos);
	}
	
	private Map<Integer, List<SimplePoint>> partitionPoints(List<SimplePoint> pts, SimplePoint depotLocation, int gradient) {
		Map<Integer, List<SimplePoint>> retVal = new HashMap<>();
		for (SimplePoint pt : pts) {
			double distance = calcDist(depotLocation, pt);
			int group = (int) Math.ceil(distance / gradient);
			if (retVal.containsKey(group)) {
				retVal.get(group).add(pt);
			} else {
				retVal.put(group, new ArrayList<SimplePoint>());
				retVal.get(group).add(pt);
			}

		}
		return retVal;
	}
	
	private RoutePlan doGA(final int populationSize, int numIterations, int carCount, List<SimplePoint> shipmentPoints,
			boolean display)
			throws InterruptedException {
		// Set initial capacity to population size in order to do less resize
		final List<RoutePlan> population = new ArrayList<RoutePlan>(populationSize);
		final List<RoutePlan> tempPopulation = new ArrayList<RoutePlan>(populationSize * methodCount);
		long globalMin = Long.MAX_VALUE;
		RoutePlan optimalPlan = null;

		// First, initialize the population.
		for (int i = 0; i < populationSize; i++) {
			RoutePlan candidate = new RoutePlan();
			candidate.load();
			population.add(candidate);
		}

		for (int i = 0; i < numIterations; i++) {
			long iterationMin = Long.MAX_VALUE;
			tempPopulation.clear();
			int firstInsertionPointInit = new Random().nextInt(shipmentPoints.size());
			int secondInsertionPointInit = new Random().nextInt(shipmentPoints.size());
			
			while (firstInsertionPointInit == secondInsertionPointInit) {
				secondInsertionPointInit = new Random().nextInt(shipmentPoints.size());
			}
			
			// Swap values if reverse
			if (firstInsertionPointInit > secondInsertionPointInit) {
				firstInsertionPointInit = firstInsertionPointInit ^ secondInsertionPointInit;
				secondInsertionPointInit = firstInsertionPointInit ^ secondInsertionPointInit;
				firstInsertionPointInit = firstInsertionPointInit ^ secondInsertionPointInit;
			}
			
			final int firstInsertionPoint = firstInsertionPointInit;
			final int secondInsertionPoint = secondInsertionPointInit;
				
			// Whether or not to add more methods for alteration
			
			Thread t1 = new Thread(new Runnable() {
				
				@Override
				public void run() {
					for (int popIndex = 0; popIndex < populationSize; popIndex++) {
						RoutePlan curRP = population.get(popIndex);
						final List<SimplePoint> curPopSP = curRP.points;
						final List<Integer> curBreak = curRP.breaks;
						tempPopulation.add(curRP);
						// Flip
						ArrayList<SimplePoint> tempSPForFlipping = new ArrayList<SimplePoint>();
						ArrayList<Integer> tempBreaksForFlipping = new ArrayList<Integer>();
						tempSPForFlipping.addAll(curPopSP);
						tempBreaksForFlipping.addAll(curBreak);
						for (int index = firstInsertionPoint; index <= secondInsertionPoint; index++) {
							tempSPForFlipping.set(firstInsertionPoint + 
									secondInsertionPoint - index, curPopSP.get(index));
						}
						tempPopulation.add(new RoutePlan(tempSPForFlipping, tempBreaksForFlipping, depotLocation));
					}
				}
			});
				
			Thread t2 = new Thread(new Runnable() {
				
				@Override
				public void run() {
					for (int popIndex = 0; popIndex < populationSize; popIndex++) {
						RoutePlan curRP = population.get(popIndex);
						final List<SimplePoint> curPopSP = curRP.points;
						final List<Integer> curBreak = curRP.breaks;
						tempPopulation.add(curRP);
						// Swap
						ArrayList<SimplePoint> tempSPForSwapping = new ArrayList<SimplePoint>();
						ArrayList<Integer> tempBreaksForSwapping = new ArrayList<Integer>();
						tempSPForSwapping.addAll(curPopSP);
						tempBreaksForSwapping.addAll(curBreak);
						tempSPForSwapping.set(firstInsertionPoint, curPopSP.get(secondInsertionPoint));
						tempSPForSwapping.set(secondInsertionPoint, curPopSP.get(firstInsertionPoint));
						tempPopulation.add(new RoutePlan(tempSPForSwapping, tempBreaksForSwapping, depotLocation));
					}
				}
			});
				
			Thread t3 = new Thread(new Runnable() {
				
				@Override
				public void run() {
					for (int popIndex = 0; popIndex < populationSize; popIndex++) {
						RoutePlan curRP = population.get(popIndex);
						final List<SimplePoint> curPopSP = curRP.points;
						final List<Integer> curBreak = curRP.breaks;
						tempPopulation.add(curRP);
						// Slide
						ArrayList<SimplePoint> tempSPForSliding = new ArrayList<SimplePoint>();
						ArrayList<Integer> tempBreaksForSliding = new ArrayList<Integer>();
						tempSPForSliding.addAll(curPopSP);
						tempBreaksForSliding.addAll(curBreak);
						for (int index = firstInsertionPoint + 1; index <= secondInsertionPoint; index++) {
							tempSPForSliding.set(index, curPopSP.get(index - 1));
						}
						tempSPForSliding.set(firstInsertionPoint, curPopSP.get(secondInsertionPoint));
						tempPopulation.add(new RoutePlan(tempSPForSliding, tempBreaksForSliding, depotLocation));
					}
				}
			});
				
			Thread t4 = new Thread(new Runnable() {
				public void run() {
					for (int popIndex = 0; popIndex < populationSize; popIndex++) {
						RoutePlan curRP = population.get(popIndex);
						final List<SimplePoint> curPopSP = curRP.points;
						tempPopulation.add(curRP);
						// Modify breaks
						ArrayList<SimplePoint> tempSPForMB = new ArrayList<SimplePoint>();
						tempSPForMB.addAll(curPopSP);
						RoutePlan MBCandidate = new RoutePlan(tempSPForMB, new ArrayList<Integer>(), depotLocation);
						MBCandidate.randomBreaks();
						tempPopulation.add(MBCandidate);
					}
				}
			});
				
			Thread t5 = new Thread(new Runnable() {
				
				@Override
				public void run() {
					for (int popIndex = 0; popIndex < populationSize; popIndex++) {
						RoutePlan curRP = population.get(popIndex);
						final List<SimplePoint> curPopSP = curRP.points;
						final List<Integer> curBreak = curRP.breaks;
						tempPopulation.add(curRP);
						// Flip & Modify breaks
						ArrayList<SimplePoint> tempSPForFlipping = new ArrayList<SimplePoint>();
						ArrayList<Integer> tempBreaksForFlipping = new ArrayList<Integer>();
						tempSPForFlipping.addAll(curPopSP);
						tempBreaksForFlipping.addAll(curBreak);
						for (int index = firstInsertionPoint; index <= secondInsertionPoint; index++) {
							tempSPForFlipping.set(firstInsertionPoint + 
									secondInsertionPoint - index, curPopSP.get(index));
						}
						RoutePlan MBCandidate = new RoutePlan(tempSPForFlipping, tempBreaksForFlipping, depotLocation);
						MBCandidate.randomBreaks();
						tempPopulation.add(MBCandidate);
					}
				}
			});
			
			Thread t6 = new Thread(new Runnable() {
				
				@Override
				public void run() {
					for (int popIndex = 0; popIndex < populationSize; popIndex++) {
						RoutePlan curRP = population.get(popIndex);
						final List<SimplePoint> curPopSP = curRP.points;
						final List<Integer> curBreak = curRP.breaks;
						tempPopulation.add(curRP);
						// Swap & Modify breaks
						ArrayList<SimplePoint> tempSPForSwapping = new ArrayList<SimplePoint>();
						ArrayList<Integer> tempBreaksForSwapping = new ArrayList<Integer>();
						tempSPForSwapping.addAll(curPopSP);
						tempBreaksForSwapping.addAll(curBreak);
						tempSPForSwapping.set(firstInsertionPoint, curPopSP.get(secondInsertionPoint));
						tempSPForSwapping.set(secondInsertionPoint, curPopSP.get(firstInsertionPoint));
						RoutePlan MBCandidate = new RoutePlan(tempSPForSwapping, tempBreaksForSwapping, depotLocation);
						MBCandidate.randomBreaks();
						tempPopulation.add(MBCandidate);
					}
				}
			});
				
			Thread t7 = new Thread(new Runnable() {
				public void run() {
					for (int popIndex = 0; popIndex < populationSize; popIndex++) {
						RoutePlan curRP = population.get(popIndex);
						final List<SimplePoint> curPopSP = curRP.points;
						final List<Integer> curBreak = curRP.breaks;
						tempPopulation.add(curRP);
						// Slide & Modify breaks
						ArrayList<SimplePoint> tempSPForSliding = new ArrayList<SimplePoint>();
						ArrayList<Integer> tempBreaksForSliding = new ArrayList<Integer>();
						tempSPForSliding.addAll(curPopSP);
						tempBreaksForSliding.addAll(curBreak);
						for (int index = firstInsertionPoint + 1; index <= secondInsertionPoint; index++) {
							tempSPForSliding.set(index, curPopSP.get(index - 1));
						}
						tempSPForSliding.set(firstInsertionPoint, curPopSP.get(secondInsertionPoint));
						RoutePlan MBCandidate = new RoutePlan(tempSPForSliding, tempBreaksForSliding, depotLocation);
						MBCandidate.randomBreaks();
						tempPopulation.add(MBCandidate);
					}
				}
			});
			
			t1.start();
			t1.join();
			t2.start();
			t2.join();
			t3.start();
			t3.join();
			t4.start();
			t4.join();
			t5.start();
			t5.join();
			t6.start();
			t6.join();
			t7.start();
			t7.join();
			
			Collections.shuffle(tempPopulation);
			population.clear();
			for (int split = 0; split < populationSize; split++) {
				long curMinCost = Long.MAX_VALUE;
				int curMinCostIndex = -1;
				
				// Whether or not to choose the max (MAY CHOOSE MAX WITH SOME PROBABILITY)
				for (int subIndex = 0; subIndex < methodCount; subIndex++) {
					int curCost = tempPopulation.get(split * methodCount + subIndex).getTotalCost();
					if (curCost < curMinCost) {
						curMinCost = curCost;
						curMinCostIndex = subIndex;
					}
				}
				
				if (curMinCost < globalMin) {
					globalMin = curMinCost;
					optimalPlan = tempPopulation.get(split * methodCount + curMinCostIndex);
				}
				
				if (curMinCost < iterationMin) {
					iterationMin = curMinCost;
				}
				population.add(tempPopulation.get(split * methodCount + curMinCostIndex));
			}
			
			if (display) {
				System.out.println("Global min is: " + globalMin);
			} else {
				if (i % 100 == 0)
					System.out.println("Iteration count: " + i);
			}
			
			rr.paintAll(optimalPlan, (Graphics2D) rr.getGraphics());
		}
//		resolveAllCrossings(optimalPlan);

//		rr.paintAll(optimalPlan, (Graphics2D) rr.getGraphics());
//		System.out.println("Cost after doing resolving crossings: " + optimalPlan.getTotalCost());
//		Thread.sleep(10000);
		return optimalPlan;
	}
	
	public static void resolveAllCrossings(RoutePlan optimalPlan) {
		System.out.println("working hard!!");
		int lastBreak = 0;
		for (int breakIndex = 0; breakIndex < optimalPlan.breaks.size(); breakIndex++) {
			resolveCrossings(optimalPlan.points, optimalPlan.depotLocation, 
					lastBreak, optimalPlan.breaks.get(breakIndex));
			lastBreak = optimalPlan.breaks.get(breakIndex);
		}
		
		if (!optimalPlan.breaks.isEmpty()) {
			resolveCrossings(optimalPlan.points, optimalPlan.depotLocation, 
					optimalPlan.breaks.get(optimalPlan.breaks.size() - 1) + 1, 
					optimalPlan.points.size() - 1);
		}
		
		System.out.println("Done!!");
	}
	
	public void loadPointsFromFile(String filename) throws IOException {
		File infile = new File(filename);
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(infile)));
		String inline = "";
		while ((inline = br.readLine()) != null) {
			String[] latln = inline.split(" ");
			assert latln.length == 2;
			SimplePoint pt = new SimplePoint(Integer.parseInt(latln[0]), Integer.parseInt(latln[1]));
			destLocations.add(pt);
		}
		br.close();
	}
	
	private void populateLocations() {
		destLocations = new ArrayList<SimplePoint>();
		Random rnd1 = new Random();
		int depotxpos = rnd1.nextInt(mapWidth);
		int depotypos = rnd1.nextInt(mapHeight);
		if (depotLocation == null)
			depotLocation = new SimplePoint(depotxpos, depotypos);

		for (int i = 0; i < numDestinations; i++) {
			SimplePoint candidate;
			do {
				Random rnd = new Random();
				int xpos = rnd.nextInt(mapWidth);
				int ypos = rnd.nextInt(mapHeight);
				candidate = new SimplePoint(xpos, ypos);
			} while (destLocations.contains(candidate) || candidate.equals(depotLocation));
			
			destLocations.add(candidate);
		}
		
//		try {
//			writePointsToFile(destLocations);
//		} catch (FileNotFoundException e) {
//			System.out.println("Unable to write points to file!");
//		}
	}
	
	public void visualize() {
		for (int xindex = 0; xindex < mapWidth; xindex++) {
			for (int yindex = 0; yindex < mapHeight; yindex++) {
				SimplePoint pt = new SimplePoint(xindex, yindex);
				if (destLocations.contains(pt)) {
					System.out.print('E');
				} else if (pt.equals(depotLocation)) {
					System.out.print('D');
				} else {
					System.out.print('-');
				}
			}
			System.out.println();
		}
	}
	
	public static void writePointsToFile(List<SimplePoint> sp) throws FileNotFoundException {
		long currentTime = System.currentTimeMillis();
		PrintWriter pr = new PrintWriter(baseDir + pointsSuffix + currentTime + ".txt");
		for (SimplePoint point : sp) {
			pr.println(point.xpos + " " + point.ypos);
		}
		pr.close();
	}
	
	public static void printRoutes(RoutePlan plan) {
		List<Integer> breaks = plan.breaks;
		List<SimplePoint> perm = plan.points;
		System.out.println(String.format("Depot Location: XPos -- %d, YPos -- %d",
				depotLocation.xpos, depotLocation.ypos));
		Collections.sort(breaks);
		int lastBreak = 0;
		for (int i = 0; i < breaks.size(); i++) {
			System.out.println(String.format("Route %s:", i));
			for (int j = lastBreak; j < breaks.get(i); j++) {
				System.out.println(String.format("XPosition: %s, YPosition: %s",
						perm.get(j).xpos, perm.get(j).ypos));
			}
			System.out.println();
			lastBreak = breaks.get(i);
		}
		
		System.out.println(String.format("Route %s:", breaks.size()));
		for (int i = lastBreak; i < perm.size(); i++) {
			System.out.println(String.format("XPosition: %s, YPosition: %s",
					perm.get(i).xpos, perm.get(i).ypos));
		}
		System.out.println();
		System.out.println(String.format("Total Cost: %d", plan.getTotalCost()));
		System.out.println();
	}
	
	public static long calcDist(SimplePoint pointA, SimplePoint pointB) {
		return Math.round(Math.sqrt(Math.pow(pointA.xpos - pointB.xpos, 2)
				+ Math.pow(pointA.ypos - pointB.ypos, 2)));
	}
	
	public static class RoutePlan {
		private List<SimplePoint> points = new ArrayList<SimplePoint>();
		private List<Integer> breaks = new ArrayList<Integer>();
		private SimplePoint depotLocation;
		
		public RoutePlan() { this.depotLocation = MainTest.depotLocation; }

		public RoutePlan(List<SimplePoint> points, List<Integer> breaks, SimplePoint depotLocation) {
			this.depotLocation = depotLocation;
			this.points = points;
			this.breaks = breaks;
		}
		
		public int getTotalCost() {
			Collections.sort(breaks);
			int sum = 0;
			int lastBreak = 0;
			for (int i = 0; i < breaks.size(); i++) {
				sum += calcDist(points.get(lastBreak), depotLocation);
				sum += calcDist(points.get(breaks.get(i) - 1), depotLocation);
				for (int j = lastBreak + 1; j < breaks.get(i); j++) {
					sum += calcDist(points.get(j), points.get(j - 1));
				}
				lastBreak = breaks.get(i);
			}
			
			sum += calcDist(points.get(lastBreak), depotLocation);
			sum += calcDist(points.get(points.size() - 1), depotLocation);
			for (int i = lastBreak + 1; i < points.size(); i++) {
				sum += calcDist(points.get(i), points.get(i - 1));
			}

			return sum;
		}
		
		public void printAllPointsAndBreaks() {
			System.out.println(String.format("Depot location: XPos: %s, YPos: %s",
					depotLocation.xpos, depotLocation.ypos));
			for (SimplePoint pt : points) {
				System.out.println(String.format("XPos: %s, YPos: %s", pt.xpos, pt.ypos));
			}

			for (Integer breaksss : breaks) {
				System.out.println(String.format("Index: %s", breaksss));
			}
		}
		
		private void load() {
			points.clear();
			breaks.clear();
			while (breaks.size() < numCars - 1) {
				int randBreak = 1 + new Random().nextInt(numDestinations - 1);
				if (!breaks.contains(randBreak)) {
					breaks.add(randBreak);
				}
			}
			
			points.addAll(destLocations);
			Collections.shuffle(points);
		}
		
		private void randomBreaks() {
			breaks.clear();
			if (numCars <= 1)
				return;

			// no constraints at all
			if (minTour < 2 && maxTour < 0) {
				while (breaks.size() < numCars - 1) {
					int randBreak = 1 + new Random().nextInt(numDestinations - 1);
					if (!breaks.contains(randBreak)) {
						breaks.add(randBreak);
					}
				}
			} else if (minTour > 1 && maxTour < 0) {
				// Only minTour needs to be considered
				for (int i = 0; i < numCars - 1; i++) {
					breaks.add(minTour);
				}
				
				int degreeOfFreedom = numDestinations - numCars * minTour;
				while (degreeOfFreedom > 0) {
					int randomIndex = new Random().nextInt(numCars);
					if (randomIndex < breaks.size())
						breaks.set(randomIndex, breaks.get(randomIndex) + 1);
					degreeOfFreedom --;
				}
				
				int sum = breaks.get(0);
				for (int i = 1; i < breaks.size(); i++) {
					sum += breaks.get(i);
					breaks.set(i, sum);
				}
			} else {
				// Both minTour and maxTour need to be considered
				for (int i = 0; i < numCars - 1; i++) {
					breaks.add(minTour);
				}
				
				int degreeOfFreedom = numDestinations - numCars * minTour;
				while (degreeOfFreedom > 0) {
					int discarded = 0;
					int randomIndex = 0;
					do {
						randomIndex = new Random().nextInt(numCars);
					} while (randomIndex >= breaks.size() ? discarded >= maxTour : 
						breaks.get(randomIndex) >= maxTour);
					if (randomIndex >= breaks.size())
						discarded ++;
					else
						breaks.set(randomIndex, breaks.get(randomIndex) + 1);
					degreeOfFreedom --;
				}
				
				breaks.set(0, breaks.get(0) - 1);
				int sum = breaks.get(0);
				for (int i = 1; i < breaks.size(); i++) {
					sum += breaks.get(i);
					breaks.set(i, sum);
				}
			}
		}

		public List<SimplePoint> getPoints() {
			return points;
		}

		public void setPoints(List<SimplePoint> points) {
			this.points = points;
		}

		public List<Integer> getBreaks() {
			return breaks;
		}

		public void setBreaks(List<Integer> breaks) {
			this.breaks = breaks;
		}
		
		public SimplePoint getDepot() {
			return depotLocation;
		}
	}
	
	public class SimplePoint {
		private int xpos;
		private int ypos;
		
		public SimplePoint(int xpos, int ypos) {
			this.xpos = xpos;
			this.ypos = ypos;
		}
		
		public int getX() {
			return xpos;
		}
		
		public int getY() {
			return ypos;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + xpos;
			result = prime * result + ypos;
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			SimplePoint other = (SimplePoint) obj;
			if (xpos != other.xpos)
				return false;
			if (ypos != other.ypos)
				return false;
			return true;
		}
	}
}
