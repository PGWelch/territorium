package com.opendoorlogistics.territorium.examples;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.logging.Logger;

import com.opendoorlogistics.territorium.SVGWriter;
import com.opendoorlogistics.territorium.problem.data.ArrayBasedTravelMatrix;
import com.opendoorlogistics.territorium.problem.data.Cluster;
import com.opendoorlogistics.territorium.problem.data.Customer;
import com.opendoorlogistics.territorium.problem.data.DistanceTime;
import com.opendoorlogistics.territorium.problem.data.Problem;
import com.opendoorlogistics.territorium.problem.data.XYLocation;
import com.opendoorlogistics.territorium.solver.ContinueCallback;
import com.opendoorlogistics.territorium.solver.ImmutableSolution;
import com.opendoorlogistics.territorium.solver.Solver;
import com.opendoorlogistics.territorium.solver.SolverConfig;
import com.opendoorlogistics.territorium.solver.SolverStateSummary;
import com.opendoorlogistics.territorium.utils.StringUtils;

public class XYMinMaxQuantitiesHeterogeneousClusters {
	private static final Logger LOGGER = Logger.getLogger(XYMinMaxQuantitiesHeterogeneousClusters.class.getName());
	private int nbCustomers=100;
	private int nbClusters=10;
	private double maxQuantity=100;
	private double minQuantity=1;
	private int quantityDistributionRandPower=3;
	private double totalClusterCapacityMultiplier = 1.2;
	private double totalClusterMinQuantityMultiplier = 0.8;
	
	public XYMinMaxQuantitiesHeterogeneousClusters setNbCustomers(int nb){
		nbCustomers = nb;
		return this;
	}

	
	public XYMinMaxQuantitiesHeterogeneousClusters setQuantityDistributionRandPower(int quantityDistributionRandPower) {
		this.quantityDistributionRandPower = quantityDistributionRandPower;
		return this;
	}


	public XYMinMaxQuantitiesHeterogeneousClusters setTotalClusterCapacityMultiplier(double totalClusterCapacityMultiplier) {
		this.totalClusterCapacityMultiplier = totalClusterCapacityMultiplier;
		return this;
	}


	public XYMinMaxQuantitiesHeterogeneousClusters setTotalClusterMinQuantityMultiplier(double totalClusterMinQuantityMultiplier) {
		this.totalClusterMinQuantityMultiplier = totalClusterMinQuantityMultiplier;
		return this;
	}


	public XYMinMaxQuantitiesHeterogeneousClusters setNbClusters(int nb){
		nbClusters = nb;
		return this;
	}
	
	
	public XYMinMaxQuantitiesHeterogeneousClusters setMaxQuantity(double val){
		maxQuantity = val;
		return this;
	}

	public XYMinMaxQuantitiesHeterogeneousClusters setMinQuantity(double val){
		minQuantity = val;
		return this;
	}
	

	
	public Problem build(Random random){
		// generate customers with lots of variation in quantity
		int locIndx=0;
		Problem problem = new Problem();
		double sumQuantity=0;
		double quantityRange = maxQuantity - minQuantity;
		for(int i =0 ; i< nbCustomers ; i++){
			Customer customer = new Customer();
			
			// quantity
			double rand = 1;
			for(int j=0;j<quantityDistributionRandPower;j++){
				rand *= random.nextDouble();
			}
			customer.setQuantity( minQuantity + quantityRange * rand);
			sumQuantity += customer.getQuantity();
			
			// location
			XYLocation xyLocation = new XYLocation();
			xyLocation.setIndex(locIndx++);
			xyLocation.setX(random.nextDouble()-0.5);
			xyLocation.setY(random.nextDouble()-0.5);
			customer.setLocation(xyLocation);

			// costs
			customer.setCostPerUnitDistance(1);
			customer.setCostPerUnitTime(1);

			problem.getCustomers().add(customer);
			
	//		LOGGER.info("Created customer with quantity " + customer.getQuantity() + " at x=" + xyLocation.getX() + " y=" + xyLocation.getY());
		}
		
		// create matrix
		ArrayBasedTravelMatrix matrix = new ArrayBasedTravelMatrix(locIndx-1);
		for(Customer from : problem.getCustomers()){
			XYLocation xyFrom = (XYLocation)from.getLocation();
			for(Customer to : problem.getCustomers()){
				XYLocation xyTo = (XYLocation)to.getLocation();		
				DistanceTime dt = new DistanceTime();
				double dx = xyFrom.getX() - xyTo.getX();
				double dy = xyFrom.getY() - xyTo.getY();
				dt.setDistance(Math.sqrt(dx*dx+dy*dy));
				dt.setTime(dt.getDistance());
				matrix.set(xyFrom.getIndex(), xyTo.getIndex(), dt);
			}		
		}
		problem.setTravelMatrix(matrix);
		
		// create input clusters
		double totalCapacity = sumQuantity * totalClusterCapacityMultiplier;
		double totalMin = sumQuantity * totalClusterMinQuantityMultiplier;
		for(int i =0 ; i< nbClusters ; i++){
			Cluster cluster = new Cluster();
			cluster.setMinQuantity(totalMin/nbClusters);
			cluster.setCapacity(totalCapacity/nbClusters);
			problem.getClusters().add(cluster);
		}
		
		return problem;
	}
	
	public static void main(String []args){
		StringUtils.setOneLineLogging();
		
		// keep same problem
		Random random = new Random(123);
		Problem problem = new XYMinMaxQuantitiesHeterogeneousClusters().setNbCustomers(2000).setNbClusters(50).build(random);
	
		// seed solver differently each time
		random = new Random();
		Solver solver = new Solver(problem, new SolverConfig(), new ContinueCallback() {
			
			@Override
			public ContinueOption continueOptimisation(SolverStateSummary stateSummary) {
				System.out.println(
					LocalDateTime.now().toString() + " - "
				+ stateSummary.getNbOuterSteps() +" - "
				+ 	(stateSummary.getBestSolution()!=null?stateSummary.getBestSolution().getCost().toSingleLineSummary() + " from " + stateSummary.getBestSolutionSource() + ", ":"")
			 + stateSummary.getOperationsStack().toString());
				return stateSummary.getNbOuterSteps() < 1000? ContinueOption.KEEP_GOING: ContinueOption.FINISH_NOW;
			}
		}, random);
		
		ImmutableSolution sol = solver.solve(null);
		
		String SVG = SVGWriter.exportXYProblem(500, problem, sol);
		StringUtils.setClipboard(SVG);
		
	}
}
