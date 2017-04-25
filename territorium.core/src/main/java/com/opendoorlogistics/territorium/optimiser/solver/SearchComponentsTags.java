package com.opendoorlogistics.territorium.optimiser.solver;

import java.util.LinkedHashSet;

public class SearchComponentsTags {
	private LinkedHashSet<TagType> tags = new LinkedHashSet<>();
	
	
	public LinkedHashSet<TagType> getTags() {
		return tags;
	}

	public SearchComponentsTags(TagType ...types){
		addTags(types);
	}
	
	public SearchComponentsTags(SearchComponentsTags deepCopyThis){
		if(deepCopyThis!=null){
			for(TagType type : deepCopyThis.getTags()){
				addTag(type);
			}
			
		}
	}
	
	public void setTags(LinkedHashSet<TagType> tags) {
		this.tags = tags;
	}

	/**
	 * Add a tag. Tag is only added if it doesn't already exist
	 * @param type
	 * @return
	 */
	public SearchComponentsTags addTag(TagType type){
		tags.add(type);
		return this;
	}

	public SearchComponentsTags addTags(TagType ...types){
		for(TagType type: types){
			addTag(type);
		}
		return this;
	}

	public String getSummary(){
		StringBuilder builder = new StringBuilder();
		for(TagType type : tags){
			if(builder.length()>0){
				builder.append("+");
			}
			builder.append(type.getCode());
		}
		return builder.toString();
	}
	
	@Override
	public String toString(){
		return getSummary();
	}
	
	public enum TagType {
		INITIAL_CONSTRUCT("INIT_CSTR","Initial construction of the solution using LS"),

		INITIAL_LSOPT("INIT_LS_OPT","Initial local search on the first solution until local optimum"),

		LS_CONSTRUCT("LS_CSTR","Constructing a new solution (post-initialisation) with local search"),

		RAND_WEIGHT_LS_CONSTRUCT(
				"RW_CSTR_LS1","Constructing a new solution (post-initialisation) with random weight-based construction followed by single step local search"),

		LS_OPT("LS_OPT","Local search until local optimimum"),

		RUIN_RECREATE("RR","Ruin and recreate"),

		SPLIT("SPLIT","Splitting into subproblems then performing ruin and recreate individually on each problem"),

		COMP_TT("COMP_TT",
				"Local search and / or construction-based local search using a target travel cost below current best as the primary objective (i.e. comparator)"),

		COMP_STD("COMP_STD",
				"Local search and / or construction-based local search using the standard comparator");


		private final String description;
		private final String code;

		private TagType(String code,String description) {
			this.code = code;
			this.description = description;
		}

		public String getDescription() {
			return description;
		}

		public String getCode() {
			return code;
		}

		
	}
}
