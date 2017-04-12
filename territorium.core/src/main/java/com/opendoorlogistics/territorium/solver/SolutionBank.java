package com.opendoorlogistics.territorium.solver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import com.opendoorlogistics.territorium.problem.data.Problem;
import com.opendoorlogistics.territorium.solver.data.Cost;

public class SolutionBank {
	private final Problem problem;
	private final Random random;
	private final SolutionBankConfig config;
	private final CostComparatorWithName standardComparator;
	private final List<CostComparatorWithName> altComparators = new ArrayList<>();
	private final MutableSolution[] solutions;
	private final String[] sources;

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
		this.config = config;
		this.problem = problem;
		this.random = random;
		this.standardComparator = new CostComparatorWithName(){
			private final Comparator<Cost> std=Cost.createApproxEqualComparator();			

			@Override
			public int compare(Cost o1, Cost o2) {
				return std.compare(o1, o2);
			}

			@Override
			public String shortCode() {
				return "";
			}
		};
				

		// create an alt comparator where travel must be less than X fraction of current best
		if (config.isTravelTarget()) {
			altComparators.add(new CostComparatorWithName() {

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
				public String shortCode() {
					return "TargTrv";
				}
			});
		}
		solutions = new MutableSolution[getNbSolutionSlots()];
		sources = new String[getNbSolutionSlots()];
		Arrays.fill(sources, null);
	}

	/**
	 * Accepts the solution if better, taking a deep copy
	 * 
	 * @param newSol
	 * @param sourceSlot
	 *            Source slot index if a source slot was used. -1 if not.
	 * @return The number of slots which were improved
	 */
	public int accept(ImmutableSolution newSol, String source) {
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
				this.sources[i] = source;
			}

		}
		return nbImproved;
	}

	public int getNbSolutionSlots() {
		return 1 + altComparators.size();
	}

	public CostComparatorWithName getComparatorForSlot(int i) {
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
	
	public CostComparatorWithName getStandardComparator(){
		return standardComparator;
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
						+ (sources[i] != null ? ", src=" + sources[i] : "") + "]");
			}
		}
		return builder.toString();
	}
	
	public String getSource(int i){
		return sources[i];
	}
}
