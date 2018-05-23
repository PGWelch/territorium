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
package com.opendoorlogistics.territorium.optimiser.solver;

import com.opendoorlogistics.territorium.optimiser.components.LocalSearch.LocalSearchConfig;
import com.opendoorlogistics.territorium.optimiser.components.ProblemSplitter.ProblemSplitterConfig;
import com.opendoorlogistics.territorium.optimiser.components.RandomisedCentreSelector;
import com.opendoorlogistics.territorium.optimiser.components.RandomisedWeightBasedCustomerAssignment;
import com.opendoorlogistics.territorium.optimiser.components.Ruin.RuinConfig;
import com.opendoorlogistics.territorium.optimiser.solver.SolutionBank.SolutionBankConfig;

public class SolverConfig {
	public static int DEFAULT_NB_OUTER_STEPS = 1000;
	private long randomSeed = 123;
	private int nbOuterSteps = DEFAULT_NB_OUTER_STEPS;
	private double newSolutionFraction = 0.25;
	
	private RandomisedCentreSelector.Config centreSelector = new RandomisedCentreSelector.Config();
	private RandomisedWeightBasedCustomerAssignment.Config weightBasedAssigner = new RandomisedWeightBasedCustomerAssignment.Config ();
	private RuinConfig ruinConfig = new RuinConfig();
	private LocalSearchConfig localSearchConfig = new LocalSearchConfig();
	private SolutionBankConfig solutionBankConfig = new SolutionBankConfig();
	private ProblemSplitterConfig problemSplitterConfig = new ProblemSplitterConfig();
	private double splitProblemProbability=0.5;
	
	public RandomisedCentreSelector.Config getCentreSelector() {
		return centreSelector;
	}
	public void setCentreSelector(RandomisedCentreSelector.Config centreSelector) {
		this.centreSelector = centreSelector;
	}
	public RandomisedWeightBasedCustomerAssignment.Config getWeightBasedAssigner() {
		return weightBasedAssigner;
	}
	public void setWeightBasedAssigner(RandomisedWeightBasedCustomerAssignment.Config weightBasedAssigner) {
		this.weightBasedAssigner = weightBasedAssigner;
	}
	public long getRandomSeed() {
		return randomSeed;
	}
	public void setRandomSeed(long randomSeed) {
		this.randomSeed = randomSeed;
	}
	public int getNbOuterSteps() {
		return nbOuterSteps;
	}
	public void setNbOuterSteps(int nbOuterSteps) {
		this.nbOuterSteps = nbOuterSteps;
	}
	public RuinConfig getRuinConfig() {
		return ruinConfig;
	}
	public void setRuinConfig(RuinConfig ruinConfig) {
		this.ruinConfig = ruinConfig;
	}
	public LocalSearchConfig getLocalSearchConfig() {
		return localSearchConfig;
	}
	public void setLocalSearchConfig(LocalSearchConfig localSearchConfig) {
		this.localSearchConfig = localSearchConfig;
	}
	public double getNewSolutionFraction() {
		return newSolutionFraction;
	}
	public void setNewSolutionFraction(double newSolutionFraction) {
		this.newSolutionFraction = newSolutionFraction;
	}
	public SolutionBankConfig getSolutionBankConfig() {
		return solutionBankConfig;
	}
	public void setSolutionBankConfig(SolutionBankConfig solutionBankConfig) {
		this.solutionBankConfig = solutionBankConfig;
	}
	public ProblemSplitterConfig getProblemSplitterConfig() {
		return problemSplitterConfig;
	}
	public void setProblemSplitterConfig(ProblemSplitterConfig problemSplitterConfig) {
		this.problemSplitterConfig = problemSplitterConfig;
	}
	public double getSplitProblemProbability() {
		return splitProblemProbability;
	}
	public void setSplitProblemProbability(double splitProblemProbability) {
		this.splitProblemProbability = splitProblemProbability;
	}
	
	
	
}
