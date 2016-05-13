package main;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class MainTest {
	/**
	 * Parameters by problem definition
	 */
	private static int mapBoundNorth = 2000;
	private static int mapBoundEast = 3000;
	private static int numDestinations = 100;
	private static int randomNumIterations = 2000; // Irrelevant
	private static int numCars = 5;
	private static ShipmentPoint depotLocation;
	private static List<ShipmentPoint> destLocations;
	private static int carCost = 200;
	
	/**
	 * Storage locations
	 */
	private static String baseDir = "C:\\Users\\bml\\Desktop\\Algorithms\\MTSPGA\\";
	private static String reportSuffix = "Report\\";
	private static String pointsSuffix = "Pointset\\";
	
	/**
	 * Tunable parameters for algorithm
	 */
	private static int populationSize = 10;
	private static int numGAIterations = 1000000;
	private static int methodCount = 8;
	private static boolean useMultithreading = false;
	private static SelectionStrategy ss = SelectionStrategy.GROUP_AND_MAX;
	
	enum SelectionStrategy {
		GROUP_AND_MAX,  // group population and select max of each group
		GROUP_AND_RANDOM,  // group population and select a random instance (better
						   // ones have greater chance to be chosen
		SORT_AND_SHUFFLE,  // sort population, shuffle and take the first ones
	}
	
	public static void main(String[] args) throws InterruptedException {
		Configuration cfg = Configuration.getInstance();
		long systemCurrentTime = System.currentTimeMillis();
		MainTest mt = new MainTest();
		mt.doCalc(randomNumIterations);
		System.out.println("Total running time is: " + (System.currentTimeMillis() - systemCurrentTime));
	}
	
	private static void loadConfiguration(Configuration cfg) {
		
	}
	
	public void doCalc(int numIterations) throws InterruptedException {
		RoutePlan rp = new RoutePlan();
		populateLocations();
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
			System.out.println(rp.getTotalCost());
		}
		
		System.out.println(String.format("The minimum cost is: %d", minCost));
		System.out.println(String.format("The average cost is: %d", grandTotalCost / numIterations));
		printRoutes(rp);
		doGA();
	}
	
	private void doGA() throws InterruptedException {
		// Set initial capacity to population size in order to do less resize
		final List<RoutePlan> population = new ArrayList<RoutePlan>(populationSize);
		final List<RoutePlan> tempPopulation = new ArrayList<RoutePlan>(populationSize * methodCount);
		long globalMin = Long.MAX_VALUE;

		// First, initialize the population.
		for (int i = 0; i < populationSize; i++) {
			RoutePlan candidate = new RoutePlan();
			candidate.load();
			population.add(candidate);
		}

		for (int i = 0; i < numGAIterations; i++) {
			long iterationMin = Long.MAX_VALUE;
			tempPopulation.clear();
			int firstInsertionPointInit = new Random().nextInt(numDestinations);
			int secondInsertionPointInit = new Random().nextInt(numDestinations);
			
			while (firstInsertionPointInit == secondInsertionPointInit) {
				secondInsertionPointInit = new Random().nextInt(numDestinations);
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
						final List<ShipmentPoint> curPopSP = curRP.points;
						final List<Integer> curBreak = curRP.breaks;
						tempPopulation.add(curRP);
						// Flip
						ArrayList<ShipmentPoint> tempSPForFlipping = new ArrayList<ShipmentPoint>();
						ArrayList<Integer> tempBreaksForFlipping = new ArrayList<Integer>();
						tempSPForFlipping.addAll(curPopSP);
						tempBreaksForFlipping.addAll(curBreak);
						for (int index = firstInsertionPoint; index <= secondInsertionPoint; index++) {
							tempSPForFlipping.set(firstInsertionPoint + 
									secondInsertionPoint - index, curPopSP.get(index));
						}
						tempPopulation.add(new RoutePlan(tempSPForFlipping, tempBreaksForFlipping));
					}
				}
			});
				
			Thread t2 = new Thread(new Runnable() {
				
				@Override
				public void run() {
					for (int popIndex = 0; popIndex < populationSize; popIndex++) {
						RoutePlan curRP = population.get(popIndex);
						final List<ShipmentPoint> curPopSP = curRP.points;
						final List<Integer> curBreak = curRP.breaks;
						tempPopulation.add(curRP);
						// Swap
						ArrayList<ShipmentPoint> tempSPForSwapping = new ArrayList<ShipmentPoint>();
						ArrayList<Integer> tempBreaksForSwapping = new ArrayList<Integer>();
						tempSPForSwapping.addAll(curPopSP);
						tempBreaksForSwapping.addAll(curBreak);
						tempSPForSwapping.set(firstInsertionPoint, curPopSP.get(secondInsertionPoint));
						tempSPForSwapping.set(secondInsertionPoint, curPopSP.get(firstInsertionPoint));
						tempPopulation.add(new RoutePlan(tempSPForSwapping, tempBreaksForSwapping));
					}
				}
			});
				
			Thread t3 = new Thread(new Runnable() {
				
				@Override
				public void run() {
					for (int popIndex = 0; popIndex < populationSize; popIndex++) {
						RoutePlan curRP = population.get(popIndex);
						final List<ShipmentPoint> curPopSP = curRP.points;
						final List<Integer> curBreak = curRP.breaks;
						tempPopulation.add(curRP);
						// Slide
						ArrayList<ShipmentPoint> tempSPForSliding = new ArrayList<ShipmentPoint>();
						ArrayList<Integer> tempBreaksForSliding = new ArrayList<Integer>();
						tempSPForSliding.addAll(curPopSP);
						tempBreaksForSliding.addAll(curBreak);
						for (int index = firstInsertionPoint + 1; index <= secondInsertionPoint; index++) {
							tempSPForSliding.set(index, curPopSP.get(index - 1));
						}
						tempSPForSliding.set(firstInsertionPoint, curPopSP.get(secondInsertionPoint));
						tempPopulation.add(new RoutePlan(tempSPForSliding, tempBreaksForSliding));
					}
				}
			});
				
			Thread t4 = new Thread(new Runnable() {
				public void run() {
					for (int popIndex = 0; popIndex < populationSize; popIndex++) {
						RoutePlan curRP = population.get(popIndex);
						final List<ShipmentPoint> curPopSP = curRP.points;
						tempPopulation.add(curRP);
						// Modify breaks
						ArrayList<ShipmentPoint> tempSPForMB = new ArrayList<ShipmentPoint>();
						tempSPForMB.addAll(curPopSP);
						RoutePlan MBCandidate = new RoutePlan(tempSPForMB, new ArrayList<Integer>());
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
						final List<ShipmentPoint> curPopSP = curRP.points;
						final List<Integer> curBreak = curRP.breaks;
						tempPopulation.add(curRP);
						// Flip & Modify breaks
						ArrayList<ShipmentPoint> tempSPForFlipping = new ArrayList<ShipmentPoint>();
						ArrayList<Integer> tempBreaksForFlipping = new ArrayList<Integer>();
						tempSPForFlipping.addAll(curPopSP);
						tempBreaksForFlipping.addAll(curBreak);
						for (int index = firstInsertionPoint; index <= secondInsertionPoint; index++) {
							tempSPForFlipping.set(firstInsertionPoint + 
									secondInsertionPoint - index, curPopSP.get(index));
						}
						RoutePlan MBCandidate = new RoutePlan(tempSPForFlipping, tempBreaksForFlipping);
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
						final List<ShipmentPoint> curPopSP = curRP.points;
						final List<Integer> curBreak = curRP.breaks;
						tempPopulation.add(curRP);
						// Swap & Modify breaks
						ArrayList<ShipmentPoint> tempSPForSwapping = new ArrayList<ShipmentPoint>();
						ArrayList<Integer> tempBreaksForSwapping = new ArrayList<Integer>();
						tempSPForSwapping.addAll(curPopSP);
						tempBreaksForSwapping.addAll(curBreak);
						tempSPForSwapping.set(firstInsertionPoint, curPopSP.get(secondInsertionPoint));
						tempSPForSwapping.set(secondInsertionPoint, curPopSP.get(firstInsertionPoint));
						RoutePlan MBCandidate = new RoutePlan(tempSPForSwapping, tempBreaksForSwapping);
						MBCandidate.randomBreaks();
						tempPopulation.add(MBCandidate);
					}
				}
			});
				
			Thread t7 = new Thread(new Runnable() {
				public void run() {
					for (int popIndex = 0; popIndex < populationSize; popIndex++) {
						RoutePlan curRP = population.get(popIndex);
						final List<ShipmentPoint> curPopSP = curRP.points;
						final List<Integer> curBreak = curRP.breaks;
						tempPopulation.add(curRP);
						// Slide & Modify breaks
						ArrayList<ShipmentPoint> tempSPForSliding = new ArrayList<ShipmentPoint>();
						ArrayList<Integer> tempBreaksForSliding = new ArrayList<Integer>();
						tempSPForSliding.addAll(curPopSP);
						tempBreaksForSliding.addAll(curBreak);
						for (int index = firstInsertionPoint + 1; index <= secondInsertionPoint; index++) {
							tempSPForSliding.set(index, curPopSP.get(index - 1));
						}
						tempSPForSliding.set(firstInsertionPoint, curPopSP.get(secondInsertionPoint));
						RoutePlan MBCandidate = new RoutePlan(tempSPForSliding, tempBreaksForSliding);
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
				}
				
				if (curMinCost < iterationMin) {
					iterationMin = curMinCost;
				}
				population.add(tempPopulation.get(split * methodCount + curMinCostIndex));
			}
			
			System.out.println("Global min is: " + globalMin);
		}
	}
	
	public void loadPointsFromFile(String filename) throws IOException {
		File infile = new File(filename);
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(infile)));
		String inline = "";
		while ((inline = br.readLine()) != null) {
			String[] latln = inline.split(" ");
			assert latln.length == 2;
			ShipmentPoint pt = new ShipmentPoint(Integer.parseInt(latln[0]), Integer.parseInt(latln[1]));
			destLocations.add(pt);
		}
		br.close();
	}
	
	private void populateLocations() {
		destLocations = new ArrayList<ShipmentPoint>();
		Random rnd1 = new Random();
		int depotxpos = rnd1.nextInt(mapBoundEast);
		int depotypos = rnd1.nextInt(mapBoundNorth);
		depotLocation = new ShipmentPoint(depotxpos, depotypos);

		for (int i = 0; i < numDestinations; i++) {
			ShipmentPoint candidate;
			do {
				Random rnd = new Random();
				int xpos = rnd.nextInt(mapBoundEast);
				int ypos = rnd.nextInt(mapBoundNorth);
				candidate = new ShipmentPoint(xpos, ypos);
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
		for (int xindex = 0; xindex < mapBoundEast; xindex++) {
			for (int yindex = 0; yindex < mapBoundNorth; yindex++) {
				ShipmentPoint pt = new ShipmentPoint(xindex, yindex);
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
	
	public static void writePointsToFile(List<ShipmentPoint> sp) throws FileNotFoundException {
		long currentTime = System.currentTimeMillis();
		PrintWriter pr = new PrintWriter(baseDir + pointsSuffix + currentTime + ".txt");
		for (ShipmentPoint point : sp) {
			pr.println(point.xpos + " " + point.ypos);
		}
		pr.close();
	}
	
	public static void printRoutes(RoutePlan plan) {
		List<Integer> breaks = plan.breaks;
		List<ShipmentPoint> perm = plan.points;
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
	
	public static long calcDist(ShipmentPoint pointA, ShipmentPoint pointB) {
		return Math.round(Math.sqrt(Math.pow(pointA.xpos - pointB.xpos, 2)
				+ Math.pow(pointA.ypos - pointB.ypos, 2)));
	}
	
	public class RoutePlan {
		private List<ShipmentPoint> points = new ArrayList<ShipmentPoint>();
		private List<Integer> breaks = new ArrayList<Integer>();
		
		public RoutePlan() {
			
		}

		public RoutePlan(List<ShipmentPoint> points, List<Integer> breaks) {
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
			for (int i = lastBreak; i < points.size(); i++) {
				sum += calcDist(points.get(i), points.get(i - 1));
			}
			
			return sum;
		}
		
		private void load() {
			points.clear();
			breaks.clear();
			while (breaks.size() < numCars) {
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
			while (breaks.size() < numCars) {
				int randBreak = 1 + new Random().nextInt(numDestinations - 1);
				if (!breaks.contains(randBreak)) {
					breaks.add(randBreak);
				}
			}
		}
	}
	
	public class ShipmentPoint {
		private int xpos;
		private int ypos;
		
		public ShipmentPoint(int xpos, int ypos) {
			this.xpos = xpos;
			this.ypos = ypos;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
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
			ShipmentPoint other = (ShipmentPoint) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (xpos != other.xpos)
				return false;
			if (ypos != other.ypos)
				return false;
			return true;
		}

		private MainTest getOuterType() {
			return MainTest.this;
		}
		
	}
}
