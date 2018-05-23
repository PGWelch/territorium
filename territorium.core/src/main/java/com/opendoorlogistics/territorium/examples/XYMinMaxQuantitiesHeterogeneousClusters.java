/*******************************************************************************
 * Copyright 2014-2017 Open Door Logistics Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.opendoorlogistics.territorium.examples;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.logging.Logger;

import com.opendoorlogistics.territorium.optimiser.data.ImmutableSolution;
import com.opendoorlogistics.territorium.optimiser.solver.ContinueCallback;
import com.opendoorlogistics.territorium.optimiser.solver.Solver;
import com.opendoorlogistics.territorium.optimiser.solver.SolverConfig;
import com.opendoorlogistics.territorium.optimiser.solver.SolverStateSummary;
import com.opendoorlogistics.territorium.problem.ArrayBasedTravelMatrix;
import com.opendoorlogistics.territorium.problem.Cluster;
import com.opendoorlogistics.territorium.problem.Customer;
import com.opendoorlogistics.territorium.problem.Problem;
import com.opendoorlogistics.territorium.problem.location.XYLocation;
import com.opendoorlogistics.territorium.utils.SVGWriter;
import com.opendoorlogistics.territorium.utils.StringUtils;

public class XYMinMaxQuantitiesHeterogeneousClusters {
	private static final Logger LOGGER = Logger.getLogger(XYMinMaxQuantitiesHeterogeneousClusters.class.getName());
	private int nbCustomers=100;
	private int nbClusters=10;
	private double maxCustomerQuantity=100;
	private double minCustomerQuantity=1;
	private int customerQuantityDistributionRandPower=3;
	private double totalClusterCapacityMultiplier = 1.2;
	private double totalClusterMinQuantityMultiplier = 0.8;
	
	public XYMinMaxQuantitiesHeterogeneousClusters setNbCustomers(int nb){
		nbCustomers = nb;
		return this;
	}

	
	public XYMinMaxQuantitiesHeterogeneousClusters setCustomerQuantityDistributionRandPower(int quantityDistributionRandPower) {
		this.customerQuantityDistributionRandPower = quantityDistributionRandPower;
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
	
	
	public XYMinMaxQuantitiesHeterogeneousClusters setMaxCustomerQuantity(double val){
		maxCustomerQuantity = val;
		return this;
	}

	public XYMinMaxQuantitiesHeterogeneousClusters setMinCustomerQuantity(double val){
		minCustomerQuantity = val;
		return this;
	}
	

	
	public Problem build(Random random){
		// generate customers with lots of variation in quantity
		int locIndx=0;
		Problem problem = new Problem();
		double sumQuantity=0;
		double quantityRange = maxCustomerQuantity - minCustomerQuantity;
		for(int i =0 ; i< nbCustomers ; i++){
			Customer customer = new Customer();
			
			// quantity
			double rand = 1;
			for(int j=0;j<customerQuantityDistributionRandPower;j++){
				rand *= random.nextDouble();
			}
			customer.setQuantity( minCustomerQuantity + quantityRange * rand);
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
		
		// create input clusters
		double totalCapacity = sumQuantity * totalClusterCapacityMultiplier;
		double totalMin = sumQuantity * totalClusterMinQuantityMultiplier;
		for(int i =0 ; i< nbClusters ; i++){
			Cluster cluster = new Cluster();
			cluster.setMinQuantity(totalMin/nbClusters);
			cluster.setMaxQuantity(totalCapacity/nbClusters);
			problem.getClusters().add(cluster);
		}

		// create matrix
		ArrayBasedTravelMatrix matrix =ArrayBasedTravelMatrix.buildForXYProblem(problem, 1);
		problem.setTravelMatrix(matrix);
		
		return problem;
	}
	
	public static void main(String []args){
		StringUtils.setOneLineLogging();
		
		// keep same problem
		Random random = new Random(123);
		Problem problem = new XYMinMaxQuantitiesHeterogeneousClusters().setNbCustomers(500).setNbClusters(10).setTotalClusterCapacityMultiplier(1.05).setTotalClusterMinQuantityMultiplier(0.95).build(random);
	
		// seed solver differently each time
		random = new Random();
		Solver solver = new Solver(problem, new SolverConfig(), new ContinueCallback() {
			
			@Override
			public ContinueOption continueOptimisation(SolverStateSummary stateSummary) {
				System.out.println(
					LocalDateTime.now().toString() + " - "
				+ stateSummary.getNbOuterSteps() +" - "
				+ "soln #" + stateSummary.getBestSolutionNb() +" "
				+ 	(stateSummary.getBestSolution()!=null?stateSummary.getBestSolution().getCost().toSingleLineSummary() + " from " + stateSummary.getBestSolutionTags() + ", ":"")
			 + stateSummary.getOperationsStack().toString());
				return stateSummary.getNbOuterSteps() < 100? ContinueOption.KEEP_GOING: ContinueOption.FINISH_NOW;
			}
		}, random);
		
		ImmutableSolution sol = solver.solve(null);
		
		String SVG = SVGWriter.exportXYProblem(500, problem, sol);
		StringUtils.setClipboard(SVG);
		
	}
}
