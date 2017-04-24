/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.territorium;

import java.awt.Component;
import java.awt.FlowLayout;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.StringConventions;
import com.opendoorlogistics.api.Tables;
import com.opendoorlogistics.api.components.ComponentConfigurationEditorAPI;
import com.opendoorlogistics.api.components.ComponentExecutionApi;
import com.opendoorlogistics.api.components.ComponentExecutionApi.ModalDialogResult;
import com.opendoorlogistics.api.components.ODLComponent;
import com.opendoorlogistics.api.components.PredefinedTags;
import com.opendoorlogistics.api.distances.ODLCostMatrix;
import com.opendoorlogistics.api.scripts.ScriptOption;
import com.opendoorlogistics.api.scripts.ScriptTemplatesBuilder;
import com.opendoorlogistics.api.scripts.ScriptTemplatesBuilder.BuildScriptCallback;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLDatastoreAlterable;
import com.opendoorlogistics.api.tables.ODLTable;
import com.opendoorlogistics.api.tables.ODLTableAlterable;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.api.tables.ODLTableDefinitionAlterable;
import com.opendoorlogistics.api.tables.beans.BeanMappedRow;
import com.opendoorlogistics.api.tables.beans.BeanTableMapping;
import com.opendoorlogistics.api.tables.beans.annotations.ODLTag;
import com.opendoorlogistics.api.ui.UIFactory.IntChangedListener;
import com.opendoorlogistics.api.ui.UIFactory.ItemChangedListener;
import com.opendoorlogistics.territorium.ScriptBuilder.ClustererScriptType;
import com.opendoorlogistics.territorium.demo.DemoAddresses;
import com.opendoorlogistics.territorium.demo.DemoBuilder;
import com.opendoorlogistics.territorium.demo.DemoConfig;
import com.opendoorlogistics.territorium.optimiser.data.Cost;
import com.opendoorlogistics.territorium.optimiser.data.ImmutableSolution;
import com.opendoorlogistics.territorium.optimiser.data.MutableSolution;
import com.opendoorlogistics.territorium.optimiser.solver.ContinueCallback;
import com.opendoorlogistics.territorium.optimiser.solver.Solver;
import com.opendoorlogistics.territorium.optimiser.solver.SolverConfig;
import com.opendoorlogistics.territorium.optimiser.solver.SolverStateSummary;
import com.opendoorlogistics.territorium.problem.Cluster;
import com.opendoorlogistics.territorium.problem.Customer;
import com.opendoorlogistics.territorium.problem.DistanceTime;
import com.opendoorlogistics.territorium.problem.Problem;
import com.opendoorlogistics.territorium.problem.TravelMatrix;
import com.opendoorlogistics.territorium.problem.location.LatLongLocation;
import com.opendoorlogistics.territorium.problem.location.LatLongLocation.LatLongLocationFactory;
import com.opendoorlogistics.territorium.problem.location.Location;

import net.xeoh.plugins.base.annotations.PluginImplementation;

@PluginImplementation
final public class TerritoriumComponent implements ODLComponent {
	public static final String UNASSIGNED = "UNASSIGNED";
	static final String COMPONENT_ID = "com.opendoorlogistics.components.territorium";
	static final int CONTINUE_OPTIMISATION_MODE = ODLComponent.MODE_FIRST_USER_MODE;
	static final int MODE_UPDATE_OUTPUT_TABLES_ONLY= CONTINUE_OPTIMISATION_MODE + 1;
//	static final int MODE_UPDATE_TABLES= MODE_UPDATE_SOLUTION_FOR_DISPLAY + 1;
	static final int MODE_BUILD_DEMO = MODE_UPDATE_OUTPUT_TABLES_ONLY + 1;

	@Override
	public String getId() {
		return COMPONENT_ID;
	}

	@Override
	public String getName() {
		return "Capacitated clusterer (territorium)";
	}

	@Override
	public ODLDatastore<? extends ODLTableDefinition> getIODsDefinition(ODLApi api, Serializable configuration) {
		Tables tables = api.tables();
		ODLDatastoreAlterable<? extends ODLTableDefinitionAlterable> ret = tables.createDefinitionDs();
		api.tables().copyTableDefinition(tables.mapBeanToTable(ODLBeanCustomer.class).getTableDefinition(), ret);
		ret.setTableName(0, "Customers");
		if (((CapClusterConfig) configuration).isUseInputClusterTable()) {
			api.tables().copyTableDefinition(tables.mapBeanToTable(ODLBeanCluster.class).getTableDefinition(), ret);
			ret.setTableName(1, "Clusters");
		}
		return ret;
	}

	@Override
	public ODLDatastore<? extends ODLTableDefinition> getOutputDsDefinition(ODLApi api, int mode,
			Serializable configuration) {
		ODLDatastoreAlterable<? extends ODLTableDefinitionAlterable> ret = api.tables().createDefinitionDs();
		api.tables().copyTableDefinition(api.tables().mapBeanToTable(ODLBeanCustomerReport.class).getTableDefinition(), ret);
		ret.setTableName(ret.getTableAt(0).getImmutableId(), "Customers_Report");

		
		api.tables().copyTableDefinition(api.tables().mapBeanToTable(ODLBeanClusterReport.class).getTableDefinition(), ret);
		ret.setTableName(ret.getTableAt(1).getImmutableId(), "Clusters_Report");
		
		api.tables().copyTableDefinition(api.tables().mapBeanToTable(ODLBeanSolutionReport.class).getTableDefinition(), ret);
		ret.setTableName(ret.getTableAt(2).getImmutableId(), "Solution_Report");
		
		
		return ret;
	}

	public static class LocationForDistancesCalculation implements BeanMappedRow {
		private long globalId;
		private String id;
		private double latitude;
		private double longitude;

		@ODLTag(PredefinedTags.LOCATION_KEY)
		public String getId() {
			return id;

		}

		public void setId(String id) {
			this.id = id;
		}

		@ODLTag(PredefinedTags.LATITUDE)
		public double getLatitude() {
			return latitude;
		}

		public void setLatitude(double latitude) {
			this.latitude = latitude;
		}

		@ODLTag(PredefinedTags.LONGITUDE)
		public double getLongitude() {
			return longitude;
		}

		public void setLongitude(double longitude) {
			this.longitude = longitude;
		}

		@Override
		public long getGlobalRowId() {
			return globalId;
		}

		@Override
		public void setGlobalRowId(long arg0) {
			this.globalId = arg0;
		}

	}

	private List<ODLBeanCustomer> getInputCustomers(ODLApi api, ODLDatastore<? extends ODLTable> ioDb) {
		return api.tables().mapBeanToTable(ODLBeanCustomer.class).readObjectsFromTable(ioDb.getTableAt(0)).stream()
				.map(o -> {
					return (ODLBeanCustomer) o;
				}).collect(Collectors.toList());

	}

	private List<ODLBeanCluster> getInputClusters(ODLApi api, ODLDatastore<? extends ODLTable> ioDb, int mode,
			List<ODLBeanCustomer> inputCustomers, CapClusterConfig config) {
		StringConventions conventions = api.stringConventions();

		class Helper {
			ODLBeanCluster createDefaultCluster(String clusterId) {
				ODLBeanCluster cluster = new ODLBeanCluster();
				cluster.setGlobalRowId(-1);
				cluster.setMinQuantity(config.getMinClusterQuantity());
				cluster.setMaxQuantity(config.getMaxClusterQuantity());
				cluster.setId(clusterId);
				return cluster;
			}
		}
		Helper helper = new Helper();

		List<ODLBeanCluster> ret = null;
		if (config.isUseInputClusterTable()) {
			ret = api.tables().mapBeanToTable(ODLBeanCluster.class).readObjectsFromTable(ioDb.getTableAt(1)).stream()
					.map(o -> {
						return (ODLBeanCluster) o;
					}).collect(Collectors.toList());

		} else {
			ret = new ArrayList<>();

			if (mode == TerritoriumComponent.MODE_UPDATE_OUTPUT_TABLES_ONLY ) {
				// use ids for clusters already present but with default min / max quantities etc
				Set<String> existingClusterIds = conventions.createStandardisedSet();
				inputCustomers.forEach(c -> {
					if (!conventions.isEmptyStandardised(c.getClusterId())) {
						existingClusterIds.add(c.getClusterId());
					}
				});
				for (String clusterId : existingClusterIds) {
					ret.add(helper.createDefaultCluster(clusterId));
				}
			} else {
				// create default clusters...
				for (int i = 0; i < config.getNumberClusters(); i++) {
					ret.add(helper.createDefaultCluster(Integer.toString(i + 1)));
				}

			}
		}

		// fill in random display colours if needed
		Random random = new Random();
		for (int i = 0; i < ret.size(); i++) {
			ODLBeanCluster cluster = ret.get(i);
			if (cluster.getDisplayColour() == null) {
				if (!conventions.isEmptyStandardised(cluster.getId())) {
					// see if we have one predefined for a number
					cluster.setDisplayColour(Utils.PREDEFINED_COLOURS_BY_NUMBER_STRING.get(cluster.getId()));
					
					// otherwise choose at random based on the cluster id string
					if(cluster.getDisplayColour()==null){
						random.setSeed(conventions.standardise(cluster.getId()).hashCode());
						cluster.setDisplayColour(Utils.randomColour(random));						
					}
				} else {
					// choose at random based on the index
					cluster.setDisplayColour(Utils.PREDEFINED_COLOURS_BY_NUMBER_STRING.get(Integer.toString(i)));

					if(cluster.getDisplayColour()==null){
						random.setSeed(i * 31 + 5);
						cluster.setDisplayColour(Utils.randomColour(random));						
					}
				}
			}
		}
		return ret;
	}

	private Problem createProblem(ComponentExecutionApi reporter, List<ODLBeanCustomer> inputCustomers,
			List<ODLBeanCluster> inputClusters, CapClusterConfig config) {
		LatLongLocationFactory llFactory = new LatLongLocationFactory();

		// read objects from the tables using bean mapping
		Problem problem = new Problem();
		problem.setCustomers(inputCustomers.stream().map(bc -> {
			Customer customer = new Customer();
			customer.setLocation(llFactory.create(bc.getLatitude(), bc.getLongitude()));
			customer.setQuantity(bc.getQuantity());
			customer.setCostPerUnitTime(bc.getCostPerUnitTravelTime());
			customer.setCostPerUnitDistance(bc.getCostPerUnitTravelDistance());
			customer.setUserIndex(bc.getGlobalRowId());
			return customer;
		}).collect(Collectors.toList()));

		
		// read clusters table
		problem.setClusters(inputClusters.stream().map(bc -> {
			Cluster cluster = new Cluster();
			cluster.setUserIndex(bc.getGlobalRowId());

			// target location
			cluster.setFixCentreToTarget(reporter.getApi().values().isTrue(bc.getFixCentreToTarget()));
			cluster.setTargetCentreCostPerUnitDistance(bc.getTargetCentreCostPerUnitDistance());
			cluster.setTargetCentreCostPerUnitTime(bc.getTargetCentreCostPerUnitTime());
			if (cluster.isFixCentreToTarget() || cluster.getTargetCentreCostPerUnitDistance() != 0
					|| cluster.getTargetCentreCostPerUnitTime() != 0) {
				cluster.setTargetCentre(llFactory.create(bc.getTargetLatitude(), bc.getTargetLongitude()));
			}

			// quantities
			cluster.setMinQuantity(bc.getMinQuantity());
			cluster.setMaxQuantity(bc.getMaxQuantity());
			return cluster;
		}).collect(Collectors.toList()));

		// build a temporary table with all locations in format required by odl distance calculation
		ArrayList<LocationForDistancesCalculation> locationRecords = new ArrayList<>();
		Problem.getAllLocations(problem).forEach(l -> {
			LatLongLocation ll = (LatLongLocation) l;
			LocationForDistancesCalculation rec = new LocationForDistancesCalculation();
			rec.setId(Integer.toString(ll.getIndex()));
			rec.setLatitude(ll.getLatitude());
			rec.setLongitude(ll.getLongitude());
			locationRecords.add(rec);
		});

		// get temporary table of all the locations and use this to create the distances table
		reporter.postStatusMessage("Generating distances");
		Tables tables = reporter.getApi().tables();
		BeanTableMapping btm = tables.mapBeanToTable(LocationForDistancesCalculation.class);
		ODLTable table = tables.createTable(btm.getTableDefinition());
		btm.writeObjectsToTable(locationRecords, table);
		ODLCostMatrix distancesTable = reporter.calculateDistances(config.getDistancesConfig(), table);
		if (reporter.isCancelled()) {
			return null;
		}

		// get a lookup of territorium location index to odl location index
		int nLocs = llFactory.getMaxLocationIndex() + 1;
		int[] territoriumToODLLocIndex = new int[nLocs];
		Arrays.fill(territoriumToODLLocIndex, -1);
		Problem.getAllLocations(problem).forEach(l -> {
			LatLongLocation ll = (LatLongLocation) l;
			territoriumToODLLocIndex[ll.getIndex()] = distancesTable.getIndex(Integer.toString(ll.getIndex()));
		});

		// create territorium matrix and get non infinite (i.e. connected) distance and time
		DoubleSummaryStatistics maxDist = new DoubleSummaryStatistics();
		DoubleSummaryStatistics maxTime = new DoubleSummaryStatistics();
		DistanceTime[][] tMatrix = new DistanceTime[nLocs][nLocs];
		for (int from = 0; from < nLocs; from++) {
			int odlFrom = territoriumToODLLocIndex[from];
			for (int to = 0; to < nLocs; to++) {
				int odlTo = territoriumToODLLocIndex[to];
				double dist = distancesTable.get(odlFrom, odlTo, ODLCostMatrix.COST_MATRIX_INDEX_DISTANCE);
				double time = distancesTable.get(odlFrom, odlTo, ODLCostMatrix.COST_MATRIX_INDEX_TIME);
				tMatrix[from][to] = new DistanceTime(dist, time);
				if (!Double.isInfinite(dist)) {
					maxDist.accept(dist);
				}
				if (!Double.isInfinite(time)) {
					maxTime.accept(time);
				}
			}
		}

		// replace and infinites with 10 x times the max
		for (int from = 0; from < nLocs; from++) {
			for (int to = 0; to < nLocs; to++) {
				if (Double.isInfinite(tMatrix[from][to].getDistance())) {
					tMatrix[from][to].setDistance(10 * maxDist.getMax());
				}
				if (Double.isInfinite(tMatrix[from][to].getTime())) {
					tMatrix[from][to].setTime(10 * maxTime.getMax());
				}

			}
		}

		problem.setTravelMatrix(new TravelMatrix() {

			@Override
			public DistanceTime get(int fromLocationIndex, int toLocationIndex) {
				return tMatrix[fromLocationIndex][toLocationIndex];
			}
		});
		return problem;
	}

	private ImmutableSolution getInitialSolution(ODLApi api, List<ODLBeanCustomer> inputCustomers,
			List<ODLBeanCluster> inputClusters, Problem problem) {
		Map<String, Integer> clusterId2Index = api.stringConventions().createStandardisedMap();
		for (int i = 0; i < inputClusters.size(); i++) {
			clusterId2Index.put(inputClusters.get(i).getId(), i);
		}

		int ncust = inputCustomers.size();
		int[] customersToClusters = new int[ncust];
		Arrays.fill(customersToClusters, -1);
		for (int i = 0; i < ncust; i++) {
			String clusterId = inputCustomers.get(i).getClusterId();
			if (clusterId != null) {
				Integer clusterIndex = clusterId2Index.get(clusterId);
				if (clusterIndex != null) {
					customersToClusters[i] = clusterIndex;
				}
			}
		}

		MutableSolution solution = new MutableSolution(problem, customersToClusters);
		return solution;
	}

	@Override
	public void execute(final ComponentExecutionApi reporter, int mode, Object configuration,
			ODLDatastore<? extends ODLTable> ioDb, ODLDatastoreAlterable<? extends ODLTableAlterable> outputDb) {

		CapClusterConfig config = (CapClusterConfig) configuration;
		if (mode == MODE_BUILD_DEMO) {
			buildDemo(reporter, config, ioDb);
			return;
		}

		reporter.postStatusMessage("Reading input information");
		ODLApi api = reporter.getApi();
		List<ODLBeanCustomer> inputCustomers = getInputCustomers(api, ioDb);
		List<ODLBeanCluster> inputClusters = getInputClusters(api, ioDb, mode, inputCustomers, config);
		Problem problem = createProblem(reporter, inputCustomers, inputClusters, config);
		if (problem == null) {
			// user cancelled
			return;
		}

		// read initial sol in case we use it
		ImmutableSolution initialSol = getInitialSolution(api, inputCustomers, inputClusters, problem);

		// create and run solver
		ImmutableSolution sol = null;
		if (mode == MODE_DEFAULT || mode == CONTINUE_OPTIMISATION_MODE) {
			reporter.postStatusMessage("Running solver");
			Solver solver = createSolver(reporter, config, problem);
			sol = solver.solve(mode == CONTINUE_OPTIMISATION_MODE ? initialSol.getCustomersToClusters() : null);
		} else {
			sol = initialSol;
		}

		// update cluster objects with solution information and write back
		if (!reporter.isCancelled() && sol != null) {
			reporter.postStatusMessage("Writing data out");

			exportCustomerReport(reporter, inputCustomers, inputClusters, sol, outputDb);
			List<ODLBeanClusterReport> exportedClusters = exportClusterReport(reporter, inputClusters, sol, outputDb);
			exportSolutionReport(reporter, inputClusters, sol, exportedClusters, outputDb);
			
			// update customers objects IF WE'RE NOT IN UPDATE MODE,
			// AS THIS MESSES UP THE UNDO / REDO BUFFER
			Tables tables = reporter.getApi().tables();
			if (mode != TerritoriumComponent.MODE_UPDATE_OUTPUT_TABLES_ONLY) {
				for (int i = 0; i < inputCustomers.size(); i++) {
					ODLBeanCustomer customer = inputCustomers.get(i);
					int clusterIndx = sol.getClusterIndex(i);
					if (clusterIndx != -1) {
						String newClusterId = inputClusters.get(clusterIndx).getId();
//						boolean update = true;
//						if(mode == TerritoriumComponent.MODE_UPDATE_TABLES && api.stringConventions().equalStandardised(newClusterId, customer.getClusterId())){
//							// don't standardised in user tables provided by the user when we don't have to...
//							update = false;
//						}
						
					//	if(update){
							customer.setClusterId(newClusterId);							
					//	}

					} else {
						customer.setClusterId(null);
					}
				}
				
				// update customer table
				BeanTableMapping customerMapping = tables.mapBeanToTable(ODLBeanCustomer.class);
				for (int i = 0; i < inputCustomers.size(); i++) {
					customerMapping.updateTableRow(inputCustomers.get(i), ioDb.getTableAt(0),
							inputCustomers.get(i).getGlobalRowId());
				}

			}




		}

		reporter.postStatusMessage("Finished clustering");
	}

	private List<ODLBeanCustomerReport> exportCustomerReport(ComponentExecutionApi reporter, List<ODLBeanCustomer>inputCustomers,
			List<ODLBeanCluster> inputClusters,
			ImmutableSolution sol, ODLDatastoreAlterable<? extends ODLTableAlterable> outputDb) {
		List<ODLBeanCustomerReport> outputCustomers = new ArrayList<>(inputCustomers.size());
		Problem problem = sol.getProblem();
		for (int i = 0; i < inputCustomers.size(); i++) {
			ODLBeanCustomerReport cr = new ODLBeanCustomerReport();
			ODLBeanCustomer c = inputCustomers.get(i);
			
			int clusterIndx = sol.getClusterIndex(i);
			if (clusterIndx != -1) {
				String newClusterId = inputClusters.get(clusterIndx).getId();
				cr.setClusterId(newClusterId);
				
			//	Cluster optCluster = problem.getClusters().get(clusterIndx);
				Location clusterLocation = sol.getClusterCentre(clusterIndx);
				Customer optCustomer = problem.getCustomers().get(i);
				cr.setTravelCost(problem.getTravelCost(clusterLocation, optCustomer));
				DistanceTime dt = problem.getTravel(clusterLocation,optCustomer);
				cr.setTravelDistance(dt.getDistance());
				cr.setTravelTime(dt.getTime());
				cr.setPCOfClusterQuantity(100 * optCustomer.getQuantity() / sol.getClusterQuantity(clusterIndx));
			} else {
				cr.setClusterId(UNASSIGNED);
			}
			
			cr.setCostPerUnitTravelDistance(c.getCostPerUnitTravelDistance());
			cr.setCostPerUnitTravelTime(c.getCostPerUnitTravelTime());
			cr.setId(c.getId());
			cr.setLatitude(c.getLatitude());
			cr.setLongitude(c.getLongitude());
			cr.setQuantity(c.getQuantity());
			

			outputCustomers.add(cr);
		}
		
		BeanTableMapping mapping = reporter.getApi().tables().mapBeanToTable(ODLBeanCustomerReport.class);			
		mapping.writeObjectsToTable(outputCustomers, outputDb.getTableAt(0));

		return outputCustomers;
	}
	
	private void exportSolutionReport(final ComponentExecutionApi reporter, List<ODLBeanCluster> inputClusters,
			ImmutableSolution sol,List<ODLBeanClusterReport> exportedClusters,
			ODLDatastoreAlterable<? extends ODLTableAlterable> outputDb){
		//List<ODLBeanClusterReport> exportedClusters 
		
		Problem problem = sol.getProblem();
		ODLBeanSolutionReport report = new ODLBeanSolutionReport();
		report.setNbAssignedCustomers(problem.getCustomers().size() - sol.getNbUnassignedCustomers());
		report.setNbUnassignedCustomers(sol.getNbUnassignedCustomers());
		report.setCost(sol.getCost().getTravel());
		report.setQuantityViolation(sol.getCost().getQuantityViolation());
		
		for(ODLBeanClusterReport cluster: exportedClusters){
			if(cluster.getId().equals(UNASSIGNED)){
				continue;
			}
			
			report.setNbNonEmptyClusters(report.getNbNonEmptyClusters()+1);
			report.setCustomerCost(report.getCustomerCost() + cluster.getSelCustomerCost());
			report.setCustomerDistance(report.getCustomerDistance() + cluster.getSelCustomerDistance());
			report.setCustomerTime(report.getCustomerTime() + cluster.getSelCustomerTime());
			report.setAssignedQuantity(report.getAssignedQuantity() + cluster.getAssignedQuantity());
			report.setTargetCost(report.getTargetCost() + cluster.getSelTargetCost());
			report.setTargetDistance(report.getTargetDistance() + cluster.getSelTargetDistance());
			report.setTargetTime(report.getTargetTime() + cluster.getSelTargetTime());
		}
				
		// We cannot set the nb of empty clusters as this is undefined if we're not using an input cluster table
		//report.setNbEmptyClusters(problem.getClusters().size() - report.getNbNonEmptyClusters());
		
		BeanTableMapping clusterMapping = reporter.getApi().tables().mapBeanToTable(ODLBeanSolutionReport.class);			
		clusterMapping.writeObjectToTable(report, outputDb.getTableAt(2));
		
	}
	
	private List<ODLBeanClusterReport> exportClusterReport(final ComponentExecutionApi reporter, List<ODLBeanCluster> inputClusters,
			ImmutableSolution sol, ODLDatastoreAlterable<? extends ODLTableAlterable> outputDb) {
		// create cluster reports objects
		Problem problem = sol.getProblem();
		List<ODLBeanClusterReport> outputClusters = new ArrayList<>(inputClusters.size()+1);
		for (int i = 0; i < inputClusters.size(); i++) {
			ODLBeanClusterReport cr = new ODLBeanClusterReport();
			
			// copy input properties
			ODLBeanCluster bc = inputClusters.get(i);
			cr.setDisplayColour(bc.getDisplayColour());
			//cr.setFixCentreToTarget(bc.getFixCentreToTarget());
			cr.setGlobalRowId(-1);
			cr.setId(bc.getId());
			cr.setMaxQuantity(bc.getMaxQuantity());
			cr.setMinQuantity(bc.getMinQuantity());
			//cr.setTargetCentreCostPerUnitDistance(bc.getTargetCentreCostPerUnitDistance());
			//cr.setTargetCentreCostPerUnitTime(bc.getTargetCentreCostPerUnitTime());
			//cr.setTargetLatitude(bc.getTargetLatitude());
			//cr.setTargetLongitude(bc.getTargetLongitude());
			
			// and output ones
			LatLongLocation centre = (LatLongLocation) sol.getClusterCentre(i);
			if (centre != null) {
				cr.setAssignedLatitude(centre.getLatitude());
				cr.setAssignedLongitude(centre.getLongitude());
			}
			cr.setAssignedQuantityViolation(sol.getClusterCost(i).getQuantityViolation());
			
			int nc = sol.getNbCustomers(i);
			cr.setAssignedCustomersCount(nc);
			cr.setAssignedQuantity(sol.getClusterQuantity(i));
			cr.setAssignedTotalCost(sol.getClusterCost(i).getTravel());
			
			// get breakdown of costs
			// cluster total customer time, distance, target time, distance, cost. Total cost.
			if(nc>0){
				Location clusterLocation = sol.getClusterCentre(i);
				double sumCustomerTime=0;
				double sumCustomerDistance=0;
				double sumCustomerCost=0;
				for(int j =0 ; j<nc ; j++){
					int customerIndx = sol.getCustomer(i, j);
					Customer optCustomer = problem.getCustomers().get(customerIndx);
					DistanceTime dt = problem.getTravel(clusterLocation, optCustomer);
					sumCustomerTime += dt.getTime();
					sumCustomerDistance += dt.getDistance();
					sumCustomerCost += problem.getTravelCost(clusterLocation, optCustomer);
				}
				cr.setSelCustomerTime(sumCustomerTime);
				cr.setSelCustomerDistance(sumCustomerDistance);
				cr.setSelCustomerCost(sumCustomerCost);

				Cluster optCluster = problem.getClusters().get(i);
				DistanceTime targetDistTime = problem.getTargetToCentreTravel(clusterLocation, optCluster);
				if(targetDistTime!=null){
					cr.setSelTargetTime(targetDistTime.getTime());
					cr.setSelTargetDistance(targetDistTime.getDistance());					
				}
				cr.setSelTargetCost(problem.getTargetToCentreTravelCost(clusterLocation, optCluster));
			}
			
			outputClusters.add(cr);
		}
		
		// include an 'unassigned' row...
		if (sol.getNbUnassignedCustomers() > 0) {
			ODLBeanClusterReport unassigned = new ODLBeanClusterReport();
			unassigned.setId(UNASSIGNED);
			unassigned.setMinQuantity(0);
			unassigned.setMaxQuantity(0);
			unassigned.setAssignedCustomersCount(sol.getNbUnassignedCustomers());
			unassigned.setGlobalRowId(-1);
			outputClusters.add(0, unassigned);
		}
		BeanTableMapping clusterMapping = reporter.getApi().tables().mapBeanToTable(ODLBeanClusterReport.class);			
		clusterMapping.writeObjectsToTable(outputClusters, outputDb.getTableAt(1));
		
		return outputClusters;
	}

	private Solver createSolver(final ComponentExecutionApi reporter, final CapClusterConfig config, Problem problem) {
		SolverConfig solverConfig = new SolverConfig();
		solverConfig.getLocalSearchConfig().setInterclusterSwaps(config.isUseSwapMoves());
		solverConfig.setNbOuterSteps(config.getMaxStepsOptimization());
		long start = System.currentTimeMillis();
		
		Solver solver = new Solver(problem, solverConfig, new ContinueCallback() {
			long lastUpdateTime = -1;
			long lastBestSolutionNb = -1;

			@Override
			public ContinueOption continueOptimisation(SolverStateSummary state) {
				if (reporter.isCancelled()) {
					return ContinueOption.USER_CANCELLED;
				}


				long currentMillis = System.currentTimeMillis();
				long ms = currentMillis - start;
				double secs = ms / 1000.0;

				if (config.getMaxSecondsOptimization() != -1 && secs > config.getMaxSecondsOptimization()) {
					return ContinueOption.FINISH_NOW;
				}

				if (reporter.isCancelled() || reporter.isFinishNow()) {
					return ContinueOption.FINISH_NOW;
				}

				// Show all details if the outer steps are slow (i.e. for big problems).
				// Assume we're running slow if we haven't had many outer steps yet.
				boolean showStepDetails = state.getOuterStepTimingsInSeconds().getCount()<2 || state.getOuterStepTimingsInSeconds().getAverage()>2; 
				
				// Callbacks will happen often, so we need to filter them...
				long millisSinceLastUpdate = currentMillis - lastUpdateTime;
				if (millisSinceLastUpdate > 200 || state.getBestSolutionNb() != lastBestSolutionNb || showStepDetails) {
					lastUpdateTime = currentMillis;
					lastBestSolutionNb = state.getBestSolutionNb();
					StringBuilder builder = new StringBuilder();
					builder.append("Running for " + (int) (ms * 0.001) + " seconds and " + state.getNbOuterSteps()
							+ " outer step(s)." + System.lineSeparator());

					if (state.getBestSolution() != null) {
						Cost cost = state.getBestSolution().getCost();
						builder.append("Current best has quantity violation=" + cost.getQuantityViolation()
								+ ", travel=" + cost.getTravel());
					}

					builder.append(System.lineSeparator());
					builder.append(System.lineSeparator());

					if(showStepDetails){
						builder.append("Operation:");
						builder.append(System.lineSeparator());
						int opNb = 0;
						for (String op : state.getOperationsStack()) {
							if (opNb > 0) {
								builder.append("->");
							}
							builder.append(op);
							opNb++;
						}
						builder.append(System.lineSeparator());						
					}
					
					reporter.postStatusMessage(builder.toString());
				}

				return ContinueOption.KEEP_GOING;
			}
		}, new Random(123));
		return solver;
	}

	@Override
	public Class<? extends Serializable> getConfigClass() {
		return CapClusterConfig.class;
	}

	@Override
	public JPanel createConfigEditorPanel(ComponentConfigurationEditorAPI factory, int mode, Serializable config,
			boolean isFixedIO) {
		return ConfigPanelFactory.create((CapClusterConfig) config, factory, isFixedIO);
		// return new ConfigPanelFactory((CapClusterConfig)config,factory,isFixedIO);
	}

	@Override
	public long getFlags(ODLApi api, int mode) {
		if (mode == MODE_UPDATE_OUTPUT_TABLES_ONLY) {
			return ODLComponent.FLAG_OUTPUT_WINDOWS_CAN_BE_SYNCHRONISED
					| ODLComponent.FLAG_ALLOW_USER_INTERACTION_WHEN_RUNNING;
		}
		return 0;
	}

	@Override
	public void registerScriptTemplates(ScriptTemplatesBuilder templatesApi) {

		for (ClustererScriptType type : ClustererScriptType.values()) {
			CapClusterConfig config = new CapClusterConfig();
			config.setUseInputClusterTable(type.inputClusters);

			templatesApi.registerTemplate(type.description, type.description, type.description,
					new TerritoriumComponent().getIODsDefinition(templatesApi.getApi(), config),
					new BuildScriptCallback() {

						@Override
						public void buildScript(ScriptOption builder) {
							new ScriptBuilder().build(type, builder);
						}

					});
		}
	}

	@Override
	public Icon getIcon(ODLApi api, int mode) {
		// Use own class loader to prevent problems if jar loaded by reflection
		return new ImageIcon(TerritoriumComponent.class.getResource("/resources/icons/capacitated-clusterer.png"));
	}

	@Override
	public boolean isModeSupported(ODLApi api, int mode) {
		return mode == ODLComponent.MODE_DEFAULT || mode == MODE_UPDATE_OUTPUT_TABLES_ONLY || mode==MODE_BUILD_DEMO;
	}

	private void buildDemo(final ComponentExecutionApi api, final CapClusterConfig conf,
			ODLDatastore<? extends ODLTable> ioDb) {
		class DemoConfigExt extends DemoConfig {
			ModalDialogResult result = null;
		}
		final DemoConfigExt demoConfig = new DemoConfigExt();

		Runnable runnable = new Runnable() {

			@Override
			public void run() {
				final JPanel panel = new JPanel();
				panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
				panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

				// add country
				String[] values = DemoAddresses.DEMO_ADDRESSES.keySet()
						.toArray(new String[DemoAddresses.DEMO_ADDRESSES.keySet().size()]);
				JPanel countryPanel = new JPanel();
				demoConfig.country = "United Kingdom";
				countryPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
				countryPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
				for (JComponent comp : api.getApi().uiFactory().createComboComponents("Country ", values,
						demoConfig.country, new ItemChangedListener<String>() {

							@Override
							public void itemChanged(String item) {
								demoConfig.country = item;
							}

						})) {
					countryPanel.add(comp);
				}
				panel.add(countryPanel);

				// add number of stops
				panel.add(api.getApi().uiFactory().createIntegerEntryPane("Number of customer points  ",
						demoConfig.nbCustomers, "", new IntChangedListener() {

							@Override
							public void intChange(int newInt) {
								demoConfig.nbCustomers = newInt;
							}
						}));

				// add number of clusters
				if (conf.isUseInputClusterTable()) {
					panel.add(api.getApi().uiFactory().createIntegerEntryPane("Number of input clusters  ",
							demoConfig.nbClusters, "", new IntChangedListener() {

								@Override
								public void intChange(int newInt) {
									demoConfig.nbClusters = newInt;
								}
							}));

				}

				// call modal
				demoConfig.result = api.showModalPanel(panel, "Select demo configuration", ModalDialogResult.OK,
						ModalDialogResult.CANCEL);
			}

			// private JCheckBox createCheck(String name, boolean selected, ActionListener listenr) {
			// JCheckBox ret = new JCheckBox(name, selected);
			// ret.addActionListener(listenr);
			// return ret;
			// }
		};

		if (SwingUtilities.isEventDispatchThread()) {
			runnable.run();
		} else {
			try {
				SwingUtilities.invokeAndWait(runnable);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		if (demoConfig.result == ModalDialogResult.OK) {
			DemoBuilder builder = new DemoBuilder(api.getApi(), demoConfig, conf, ioDb);
			builder.build();
		}
	}

}
