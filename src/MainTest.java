import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class MainTest {
	private static int mapBoundNorth = 200;
	private static int mapBoundEast = 300;
	private static int numDestinations = 10;
	private static int randomNumIterations = 20;
	private static int numCars = 4;
	private static ShipmentPoint depotLocation;
	private static List<ShipmentPoint> destLocations;
	public static void main(String[] args) {
		MainTest mt = new MainTest();
		mt.doCalc(randomNumIterations);
	}
	
	public void doCalc(int numIterations) {
		RoutePlan rp = new RoutePlan();
		populateLocations();
		List<Integer> optimalBreaks = new ArrayList<Integer>();
		int minCost = 1000000000;
		long grandTotalCost = 0;
		for (int i = 0; i < numIterations; i++) {
			rp.randomBreaks();
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
	
	private void doGA() {
		for (int i = 0; i < 1000; i++) {
			
		}
	}
	
	private void populateLocations() {
		destLocations = new ArrayList<ShipmentPoint>();
		destLocations.add(new ShipmentPoint(1,1));
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
		}
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
			
			return sum;
		}
		
		private void randomBreaks() {
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
