package com.opendoorlogistics.territorium.solver;

import java.util.Arrays;
import java.util.Random;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.opendoorlogistics.territorium.problem.data.Problem;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.set.hash.TIntHashSet;

public class Ruin {
	private final Problem problem;
	private final Random random;
	private final RuinConfig config;
	private RuinType lastRuinType;
	
	public static class RuinConfig{
		private double weightDeleteClusters = 0.4;
		private double weightDeleteCustomers = 0.4;
		//private double weightDeleteAll = 0.2;
		private double minRuinCustomersFraction = 0.25;
		private double maxRuinCustomersFraction = 0.75;
		private double minRuinClustersFraction = 0.25;
		private double maxRuinClustersFraction = 0.75;
		
		public double getWeightDeleteClusters() {
			return weightDeleteClusters;
		}
		public void setWeightDeleteClusters(double weightDeleteClusters) {
			this.weightDeleteClusters = weightDeleteClusters;
		}
		public double getWeightDeleteCustomers() {
			return weightDeleteCustomers;
		}
		public void setWeightDeleteCustomers(double weightDeleteCustomers) {
			this.weightDeleteCustomers = weightDeleteCustomers;
		}
		//public double getWeightDeleteAll() {
	//		return weightDeleteAll;
		//}
		//public void setWeightDeleteAll(double weightDeleteAll) {
		//	this.weightDeleteAll = weightDeleteAll;
		//}
		
				
		public double getMinRuinClustersFraction() {
			return minRuinClustersFraction;
		}
		public void setMinRuinClustersFraction(double minRuinClustersFraction) {
			this.minRuinClustersFraction = minRuinClustersFraction;
		}
		public double getMaxRuinClustersFraction() {
			return maxRuinClustersFraction;
		}
		public void setMaxRuinClustersFraction(double maxRuinClustersFraction) {
			this.maxRuinClustersFraction = maxRuinClustersFraction;
		}
		public double getMinRuinCustomersFraction() {
			return minRuinCustomersFraction;
		}
		public void setMinRuinCustomersFraction(double minRuinCustomersFraction) {
			this.minRuinCustomersFraction = minRuinCustomersFraction;
		}
		public double getMaxRuinCustomersFraction() {
			return maxRuinCustomersFraction;
		}
		public void setMaxRuinCustomersFraction(double maxRuinCustomersFraction) {
			this.maxRuinCustomersFraction = maxRuinCustomersFraction;
		}
		@JsonIgnore
		public double getWeight(RuinType type){
			switch (type) {
			case DELETE_CLUSTERS:
				return getWeightDeleteClusters();
				
			case DELETE_CUSTOMERS:
				return getWeightDeleteCustomers();
				
		//	case DELETE_ALL:
		//		return getWeightDeleteAll();
				
			default:
				throw new IllegalArgumentException();
			}
			
		}
		
	}
	

	public Ruin(Problem problem, RuinConfig config, Random random) {
		this.problem = problem;
		this.config = config;
		this.random = random;
	}

	// Don't bother with a delete all ruin type as the ruin-recreate will type this anyway
	public enum RuinType{
		DELETE_CLUSTERS,
		DELETE_CUSTOMERS,
	//	DELETE_ALL
	}
	
	private RuinType chooseRuin(){
		// normalise ruins
		double sum=0;
		for(RuinType type : RuinType.values()){
			if(config.getWeight(type)<0){
				throw new RuntimeException("Ruin weights cannot be negative");
			}
			sum+=config.getWeight(type);
		}

		double rand = random.nextDouble();
		double randSum=0;
		for(RuinType type : RuinType.values()){
			randSum += config.getWeight(type)/sum;
			if(rand <= randSum){
				return type;
			}
			
		}
		// this should never happen....
		return RuinType.values()[random.nextInt(RuinType.values().length)];
	}
	
	private int getNbToRuin(String type, double min, double max, int total){
		if(min<0 || max>1){
			throw new RuntimeException("Min " +type+ " fraction must be between 0 and 1");
		}

		if(max<0 || max>1){
			throw new RuntimeException("Max " + type + " fraction must be between 0 and 1");
		}
		
		if(min > max){
			throw new RuntimeException("Min " + type + " fraction cannot be greater than max " + type+" fraction");			
		}
		
		
		// choose the target fraction to remove
		double fractionToRuin = min + (max-min) * random.nextDouble();
		
		int toRemove =(int) Math.ceil(total * fractionToRuin);
		toRemove = Math.min(total, toRemove);
		return toRemove;
	}
	
	private int [] ruinCustomers(int [] assignment){

		// get loaded customers and shuffle them
		TIntArrayList loaded = new TIntArrayList();
		int nc = assignment.length;
		for(int i =0 ; i< nc ; i++){
			if(assignment[i]!=-1){
				loaded.add(i);
			}
		}
		loaded.shuffle(random);

		
		int toRemove = getNbToRuin("customer ruin", config.getMinRuinCustomersFraction(), config.getMaxRuinCustomersFraction(),loaded.size());
		
		
		// remove the fraction
		int [] ret = Arrays.copyOf(assignment, nc);				
		for(int i =0 ; i<toRemove ; i++){
			ret[loaded.get(i)]=-1;
		}

		return ret;
	}
	
	public int [] ruin(int [] assignment){
		lastRuinType = chooseRuin();
		return ruin(lastRuinType, assignment);
	}

	
	public RuinType getLastRuinType() {
		return lastRuinType;
	}

	private int [] ruinClusters(int[] assignment){
		// get clusters present and shuffle them
		TIntHashSet clusters = new TIntHashSet();
		for(int c : assignment){
			if(c!=-1){
				clusters.add(c);
			}
		}
		TIntArrayList shuffled = new TIntArrayList(clusters);
		shuffled.shuffle(random);

		// get nb clusters to remove
		int toRemove = getNbToRuin("cluster ruin", config.getMinRuinClustersFraction(), config.getMaxRuinClustersFraction(),clusters.size());
		if(shuffled.size()>=1){
			toRemove =Math.max(1, toRemove);
		}
		
		// get the set of clusters to remove
		TIntHashSet toRemoveSet = new TIntHashSet();
		for(int i =0 ; i<toRemove ; i++){
			toRemoveSet.add(shuffled.get(i));
		}
		
		int nc = assignment.length;
		int [] ret = Arrays.copyOf(assignment, nc);
		for(int i =0 ; i<nc ; i++){
			if(toRemoveSet.contains(ret[i])){
				ret[i]=-1;
			}
		}
		return ret;
	}
	
	public int[] ruin(RuinType type, int[] assignment) {
		if(assignment.length!=problem.getCustomers().size()){
			throw new IllegalArgumentException();
		}

		switch(type){
		case DELETE_CLUSTERS:
			return ruinClusters(assignment);
			
		case DELETE_CUSTOMERS:
			return ruinCustomers(assignment);
			
//		case DELETE_ALL:
//			int [] ret = new int[assignment.length];
//			Arrays.fill(ret, -1);
//			return ret;
		}
		
		throw new IllegalArgumentException();
	}
}
