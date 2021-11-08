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
package com.opendoorlogistics.territorium;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Random;

import org.junit.Test;

import com.opendoorlogistics.territorium.optimiser.data.Cost;
import com.opendoorlogistics.territorium.optimiser.data.ImmutableSolution;
import com.opendoorlogistics.territorium.optimiser.solver.Solver;
import com.opendoorlogistics.territorium.optimiser.solver.SolverConfig;
import com.opendoorlogistics.territorium.problem.ArrayBasedTravelMatrix;
import com.opendoorlogistics.territorium.problem.Cluster;
import com.opendoorlogistics.territorium.problem.Customer;
import com.opendoorlogistics.territorium.problem.DistanceTime;
import com.opendoorlogistics.territorium.problem.Problem;
import com.opendoorlogistics.territorium.problem.location.XYLocation;
import com.opendoorlogistics.territorium.problem.location.XYLocation.XYLocationFactory;

public class TestModelBehaviour {

	
	@Test
	public void testFixedCentres(){
		
		// setup a problem where half the clusters have fixed centres and are offset to make them bad
		Random random = new Random(123);
		int nbCustomers=100;
		int nbClusters=10;
		Problem problem = createProblem( random, nbCustomers, nbClusters,true);
		problem.getClusters().forEach(c->c.setFixCentreToTarget(true));
		
		SolverConfig solverConfig = new SolverConfig();
		solverConfig.setNbOuterSteps(25);
		
		// check initially only the non-fixed clusters with the better locations get customers
		Solver solver= new Solver(problem, solverConfig, null, random);
		ImmutableSolution sol = solver.solve(null);
		int totalAssigned=0;
		for(int i =0 ; i<nbClusters ; i++){
			int nbAssigned = sol.getNbCustomers(i);
			if(i%2==0){
				assertEquals("Offset clusters should initially have no customers",0, nbAssigned);
				assertNotNull(sol.getClusterCentre(i));
				Cluster cluster = problem.getClusters().get(i);
				XYLocation fixCentre = (XYLocation)cluster.getTargetCentre();
				XYLocation solCentre = (XYLocation)sol.getClusterCentre(i);
				assertTrue("Should be same object", fixCentre == solCentre);
			}
			totalAssigned+=nbAssigned;
		}
		assertEquals(nbCustomers, totalAssigned);
		
		// reduce the number of customers each cluster can serve
		// we should see travel go up dramatically to avoid the quantty violation,
		// and then go back down again when cluster sizes get to 0 and quantity violation becomes unavoidable
		Cost initialCost = new Cost(sol.getCost());
		double maxTravel=0;
		while(problem.getClusters().get(0).getMaxQuantity()>0){
			problem.getClusters().forEach(c->c.setMaxQuantity(c.getMaxQuantity()-5));
			sol = new Solver(problem, solverConfig, null, random).solve(null);
			maxTravel = Math.max(maxTravel, sol.getCost().getCost());
			for(int i =0 ; i<nbClusters ; i++){
				if(i%2==0){
					Cluster cluster = problem.getClusters().get(i);
					assertTrue("Should be same object", cluster.getTargetCentre() == sol.getClusterCentre(i));
				}
			}
			System.out.println(sol.getCost().toSingleLineSummary());
		}
		
		assertTrue( maxTravel > 10 * initialCost.getCost());
		assertTrue(sol.getCost().getCost() < 1.5 * initialCost.getCost());
	}

	private Problem createProblem(Random random, int nbCustomers,
			int nbClusters, boolean offset) {
		XYLocationFactory locationFactory = new XYLocationFactory();

		Problem problem = new Problem();
		for(int i =0 ; i<nbCustomers;i++){
			Customer customer = new Customer();
			customer.setQuantity(1);
			customer.setCostPerUnitTime(1);
			customer.setCostPerUnitDistance(0);
			customer.setLocation(locationFactory.create(random.nextDouble(), random.nextDouble()));
			problem.getCustomers().add(customer);
		}
		
		for(int i =0 ; i<nbClusters ; i++){
			Cluster cluster = new Cluster();
			double xoffset = i%2==0 && offset?10:0;
			cluster.setTargetCentre(locationFactory.create(xoffset + random.nextDouble(), random.nextDouble()));
			cluster.setFixCentreToTarget(true);
			cluster.setTargetCentreCostPerUnitDistance(0);
			cluster.setTargetCentreCostPerUnitTime(0);
			cluster.setMinQuantity(0);
			cluster.setMaxQuantity(nbCustomers);
			problem.getClusters().add(cluster);
		}
		
		problem.setTravelMatrix(ArrayBasedTravelMatrix.buildForXYProblem(problem, 1));
		return problem;
	}
	
	@Test
	public void testTargetCentres(){
		// create target centres in the same areas as the customers but set target centre cost to zero
		// set quantity so all clusters should be used
		Random random = new Random(123);
		int nbCustomers=100;
		int nbClusters=10;
		Problem problem = createProblem( random, nbCustomers, nbClusters,false);
		problem.getClusters().forEach(c->{
			c.setFixCentreToTarget(false);
			c.setTargetCentreCostPerUnitDistance(0);
			c.setTargetCentreCostPerUnitTime(0);
			c.setMaxQuantity( 1.01*nbCustomers / nbClusters);
		});
		
		SolverConfig solverConfig = new SolverConfig();
		solverConfig.setNbOuterSteps(25);

		class Helper{
			double getTotalTargetDistance(ImmutableSolution solution){
				double sum=0;
				for(int i =0 ; i<nbClusters ; i++){
					DistanceTime dt = problem.getTravelMatrix().get(problem.getClusters().get(i).getTargetCentre().getIndex(), solution.getClusterCentre(i).getIndex());
					sum += dt.getDistance();
				}
				return sum;
			}
			
			void printTotalTargetDistance(ImmutableSolution solution){
				System.out.println("CostPerUnit=" + problem.getClusters().get(0).getTargetCentreCostPerUnitDistance() + ", Total dist=" + getTotalTargetDistance(solution));
			}
		}
		Helper helper = new Helper();
		
		Double initialTargetDist=null;
		double finalTargetDist=0;
		for(double costPerUnit = 0 ; costPerUnit <5 ; costPerUnit+=0.1){
			double finalCostPerUnit = costPerUnit;
			problem.getClusters().forEach(c->{
				c.setTargetCentreCostPerUnitDistance(finalCostPerUnit);
				c.setTargetCentreCostPerUnitTime(finalCostPerUnit);
			});
			
			ImmutableSolution sol = new Solver(problem, solverConfig, null, random).solve(null);
			for(int i =0 ; i<nbClusters ; i++){
				assertTrue(sol.getNbCustomers(i)>=5);
			}
			
			if(initialTargetDist==null){
				initialTargetDist = helper.getTotalTargetDistance(sol);
			}
			finalTargetDist = helper.getTotalTargetDistance(sol);
			helper.printTotalTargetDistance(sol);
		}
		
		assertTrue(finalTargetDist < 0.2 * initialTargetDist);
	}
	
}
