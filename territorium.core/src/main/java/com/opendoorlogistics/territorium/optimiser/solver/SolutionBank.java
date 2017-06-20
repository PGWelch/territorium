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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import com.opendoorlogistics.territorium.optimiser.data.Cost;
import com.opendoorlogistics.territorium.optimiser.data.ImmutableSolution;
import com.opendoorlogistics.territorium.optimiser.data.MutableSolution;
import com.opendoorlogistics.territorium.optimiser.solver.SearchComponentsTags.TagType;
import com.opendoorlogistics.territorium.problem.Problem;

/**
 * Bank of solutions. 
 * Stores a main solution and optional alternate solutions if alternate optimisation comparators are enabled.
 * Only accepts new solutions if they are definitely improving (i.e. beyond error tolerance).
 * @author Phil
 *
 */
public class SolutionBank {
	private final Problem problem;
	private final Random random;
	private final CostComparatorWithTags standardComparator;
	private final List<CostComparatorWithTags> altComparators = new ArrayList<>();
	private final MutableSolution[] solutions;
	private final SearchComponentsTags[] sources;
	private final long [] solutionNb;
	
	public static class SolutionBankConfig {
		private double travelTargetImprovementFraction = 0.99999;
		private boolean travelTarget = true;

		public double getTravelTargetImprovementFraction() {
			return travelTargetImprovementFraction;
		}

		public void setTravelTargetImprovementFraction(double travelTargetImprovementFraction) {
			this.travelTargetImprovementFraction = travelTargetImprovementFraction;
		}

		public boolean isTravelTarget() {
			return travelTarget;
		}

		public void setTravelTarget(boolean travelTarget) {
			this.travelTarget = travelTarget;
		}

	}

	public SolutionBank(SolutionBankConfig config, Problem problem, Random random) {
		this.problem = problem;
		this.random = random;
		this.standardComparator = new CostComparatorWithTags(){
			private final Comparator<Cost> std=Cost.createApproxEqualComparator();			

			@Override
			public int compare(Cost o1, Cost o2) {
				return std.compare(o1, o2);
			}

			@Override
			public TagType []getTags() {
				return new TagType[]{TagType.COMP_STD};
			}
		};
				

		// create an alt comparator where travel must be less than X fraction of current best
		if (config.isTravelTarget()) {
			altComparators.add(new CostComparatorWithTags() {

				@Override
				public int compare(Cost a, Cost b) {
					double targetTravel = Double.POSITIVE_INFINITY;
					if (solutions[0] != null) {
						targetTravel = solutions[0].getCost().getTravel() * config.getTravelTargetImprovementFraction();
					}

					boolean aIsBelow = a.getTravel() <= targetTravel;
					boolean bIsBelow = b.getTravel() <= targetTravel;
					if (aIsBelow && !bIsBelow) {
						return -1;
					}
					if (!aIsBelow && bIsBelow) {
						return +1;
					}
					if (aIsBelow && bIsBelow) {
						return standardComparator.compare(a, b);
					}

					// both are above... penalise the one furthest above...
					double aViolation = a.getTravel() - targetTravel;
					double bViolation = b.getTravel() - targetTravel;
					if (Cost.numbersAreApproxEqual(aViolation, bViolation)) {
						return standardComparator.compare(a, b);
					}

					return aViolation < bViolation ? -1 : +1;
				}

				@Override
				public TagType[] getTags() {
					return new TagType[]{TagType.COMP_TT};
				}
			});
		}
		
		int n = getNbSolutionSlots();
		solutions = new MutableSolution[n];
		sources = new SearchComponentsTags[n];
		solutionNb = new long[n];
		for(int i =0 ; i<n;i++){
			sources[i] = new SearchComponentsTags();
		}
	}

	/**
	 * Accepts the solution if better, taking a deep copy
	 * 
	 * @param newSol
	 * @param sourceSlot
	 *            Source slot index if a source slot was used. -1 if not.
	 * @return The number of slots which were improved
	 */
	public int accept(ImmutableSolution newSol, SearchComponentsTags source) {
		if(newSol.getNbUnassignedCustomers()>0){
			throw new RuntimeException("Never add a solution to the bank with unassigned customers");
		}
		
		int nbImproved = 0;
		for (int i = 0; i < getNbSolutionSlots(); i++) {
			ImmutableSolution existing = solutions[i];
			boolean accept = existing == null;
			int diff = -1;
			if (!accept) {
				diff = getComparatorForSlot(i).compare(newSol.getCost(), existing.getCost());
				accept = diff < 0 || (diff == 0 && random.nextBoolean());
			}

			if (diff < 0) {
				nbImproved++;
			}

			if (accept) {
				// take deep copy...
				solutions[i] = new MutableSolution(problem, newSol.getCustomersToClusters());
				sources[i] = new SearchComponentsTags(source);
				solutionNb[i] = solutionNb[i]+1;
			}

		}
		return nbImproved;
	}

	public int getNbSolutionSlots() {
		return 1 + altComparators.size();
	}

	public CostComparatorWithTags getComparatorForSlot(int i) {
		return i == 0 ? standardComparator : altComparators.get(i - 1);
	}

	/**
	 * Can be null on first step
	 * 
	 * @param i
	 * @return
	 */
	public ImmutableSolution getSolutionSlot(int i) {
		return solutions[i];
	}

	public ImmutableSolution getStandardSol() {
		return solutions[0];
	}
	
	public CostComparatorWithTags getStandardComparator(){
		return standardComparator;
	}

	public long getStandardSolutionNb(){
		return solutionNb[0];
	}
	
	public String toSingleLineSummary() {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < getNbSolutionSlots(); i++) {
			if (i > 0) {
				builder.append(", ");
			}
			if (solutions[i] == null) {
				builder.append("[no sol]");
			} else {
				builder.append("[" + solutions[i].getCost().toSingleLineSummaryNoBrackets()
						+ (sources[i] != null ? ", src=" + sources[i].getSummary() : "") + "]");
			}
		}
		return builder.toString();
	}
	
	public SearchComponentsTags getSource(int i){
		return sources[i];
	}
}
