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

import java.io.Serializable;

import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.components.ODLComponent;
import com.opendoorlogistics.api.components.PredefinedTags;
import com.opendoorlogistics.api.scripts.ScriptAdapter;
import com.opendoorlogistics.api.scripts.ScriptAdapterTable;
import com.opendoorlogistics.api.scripts.ScriptComponentConfig;
import com.opendoorlogistics.api.scripts.ScriptInputTables;
import com.opendoorlogistics.api.scripts.ScriptInstruction;
import com.opendoorlogistics.api.scripts.ScriptOption;
import com.opendoorlogistics.api.scripts.ScriptOption.OutputType;
import com.opendoorlogistics.api.standardcomponents.Maps;
import com.opendoorlogistics.api.standardcomponents.TableCreator;
import com.opendoorlogistics.api.standardcomponents.TableViewer;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.territorium.utils.Pair;

/**
 * TODO... If we included a default script, we would want core features: - Start optimisation - Continue optimisation -
 * Update tables - View clusters summary - View barchart?
 * 
 * The input to this would be different depending on whether we used input clusters or not.
 * 
 * When using points, we'd want to add a map with points to this. When not using points, we'd want to add a polygon map
 * to this, and probably have options to add / update 'customers' from shapefile.
 * 
 * This gives various options, Just component (no input clusters) Just component (input clusters) Automatic setup for
 * points (no input clusters) Automatic setup for points (with input clusters) Automatic setup for polygon shapefile (no
 * input clusters) Automatic setup for polygon shapefile (with input clusters)
 * 
 * Instead of setting up programatically could we combine script templates?
 */
public class ScriptBuilder {
	public static final String NO_CONTROLS = "No controls setup";
	public static final String SAME_QUANTS = "same params for each cluster";
	public static final String DIFF_QUANTS = "separate params for each cluster";
	public static final String POINTS_OR_POLYS = "Setup for points or polygons";
	public static final String POINTS = "Setup for points";

	public enum ClustererScriptType {
		COMPONENT_ONLY_NO_INPUT_CLUSTERS(NO_CONTROLS + ", " + SAME_QUANTS, false, false, false),

		COMPONENT_ONLY_INPUT_CLUSTERS(NO_CONTROLS + ", " + DIFF_QUANTS, true, false, false),

	//	BASIC_OPTIONS_NO_INPUT_CLUSTERS(POINTS_OR_POLYS + ", " + SAME_QUANTS, false, true, false),

	//	BASIC_OPTIONS_SETUP_INPUT_CLUSTERS(POINTS_OR_POLYS + ", " + DIFF_QUANTS, true, true, false),

		POINTS_SETUP_NO_INPUT_CLUSTERS(POINTS + ", " + SAME_QUANTS, false, true, true),

		POINTS_SETUP_INPUT_CLUSTERS(POINTS + ", " + DIFF_QUANTS, true, true, true);

		private ClustererScriptType(String description, boolean inputClusters, boolean coreControls,
				boolean pointsMap) {
			this.inputClusters = inputClusters;
			this.points = pointsMap;
			this.coreControls = coreControls;
			this.description = description;
		}

		public final boolean inputClusters;
		private final boolean coreControls;
		private final boolean points;
		public final String description;
	}

	private static class ScriptBuilderBB {
		ODLApi api;
		ScriptOption builder;
		ClustererScriptType type;
		TerritoriumComponent component;
		CapClusterConfig config;
		ODLDatastore<? extends ODLTableDefinition> ioDfn;
		ODLDatastore<? extends ODLTableDefinition> outputDfn;
		ScriptAdapter inputData;
		ScriptComponentConfig settings;
		String selectedClustersTableName;
	}

	public void build(ClustererScriptType type, ScriptOption builder) {
		ScriptBuilderBB bb = new ScriptBuilderBB();
		bb.builder = builder;
		bb.type = type;
		bb.component = new TerritoriumComponent();
		bb.config = new CapClusterConfig();
		bb.config.setUseInputClusterTable(type.inputClusters);
		bb.api = builder.getApi();
		bb.selectedClustersTableName = bb.component.getOutputDsDefinition(bb.api, TerritoriumComponent.MODE_DEFAULT, bb.config)
				.getTableAt(1).getName(); 
		bb.ioDfn = bb.component.getIODsDefinition(bb.api, bb.config);
		bb.outputDfn = bb.component.getOutputDsDefinition(bb.api,ODLComponent.MODE_DEFAULT, bb.config);
		bb.inputData = builder.addDataAdapter("Input data");
		ScriptInputTables inputTables = builder.getInputTables();
		for (int i = 0; i < bb.ioDfn.getTableCount(); i++) {
			if (inputTables != null && i < inputTables.size() && inputTables.getSourceTable(i) != null) {
				// use input table if we have one
				bb.inputData.addSourcedTableToAdapter(inputTables.getSourceDatastoreId(i),
						inputTables.getSourceTable(i), bb.ioDfn.getTableAt(i));
			} else {
				// otherwise don't match to a source and just give a default source pointing to an identical table in
				// the spreadsheet
				bb.inputData.addSourcedTableToAdapter(bb.api.stringConventions().getSpreadsheetAdapterId(),
						bb.ioDfn.getTableAt(i), bb.ioDfn.getTableAt(i));
			}
		}

		// create shared stand-alone config
		bb.settings = builder.addComponentConfig("Settings", bb.component.getId(), bb.config);

		if (!type.coreControls) {
			// just add optimiser with empty output tables
			ScriptInstruction instrBuilder = addRunComponent(bb, bb.builder, ODLComponent.MODE_DEFAULT);
			instrBuilder.setName("Optimise clusters");
			addCopyOutput(bb,new Pair<ScriptOption, ScriptInstruction>(bb.builder, instrBuilder));
			return;
		}

		if (type.points) {
			ScriptOption prepare = bb.builder.addOption("Prepare data", "Prepare data");
			createTables(bb,prepare);

			String name = "Fill tables with demo data";
			prepare.addOption(name, name).addInstruction(bb.inputData.getAdapterId(),
					TerritoriumComponent.COMPONENT_ID, TerritoriumComponent.MODE_BUILD_DEMO,
					bb.settings.getComponentConfigId());
			
		}
		
		ScriptOption optimise = bb.builder.addOption("Optimise", "Optimise");
		addOptionWithRunComponent(bb, optimise,"Start optimisation", ODLComponent.MODE_DEFAULT);
		addOptionWithRunComponent(bb, optimise,"Continue optimisation", TerritoriumComponent.CONTINUE_OPTIMISATION_MODE);
		

		ScriptOption analyse = bb.builder.addOption("Analyse results", "Analyse results");
		if (type.points) {
			addPointsMap(bb,analyse);
		}
		
		addViewReportTables(bb,analyse);

		addCopyOutput(bb, addOptionWithRunComponent(bb, analyse,"Export report tables to spreadsheet", TerritoriumComponent.MODE_UPDATE_OUTPUT_TABLES_ONLY));

	}
	
	private void createTables(ScriptBuilderBB bb, ScriptOption parent){
		
		// create all empty tables
		String optionName = "Create input table(s)";
		TableCreator tableCreatorComponent =bb.api.standardComponents().tableCreator();
		ODLTableDefinition [] tables = new ODLTableDefinition [bb.ioDfn.getTableCount()];
		for(int i=0;i<tables.length ; i++){
			tables[i] = bb.ioDfn.getTableAt(i);
		}
		Serializable tableCreatorConfig = tableCreatorComponent.createConfiguration(bb.api, tables);
		ScriptOption addOption = parent.addOption(optionName, optionName);
		ScriptInstruction addInstruction = addOption.addInstruction(null, tableCreatorComponent.getId(), ODLComponent.MODE_DEFAULT, tableCreatorConfig);
		addOption.addCopyTable(addInstruction.getOutputDatastoreId(), "", OutputType.COPY_ALL_TABLES, "");
					
	}

	private Pair<ScriptOption, ScriptInstruction> addOptionWithRunComponent(ScriptBuilderBB bb,ScriptOption parentOption, String name, int mode) {
		ScriptOption optOption = parentOption.addOption(name, name);
		return new Pair<ScriptOption, ScriptInstruction>(optOption, addRunComponent(bb, optOption, mode));
		
	}

	private ScriptInstruction addRunComponent(ScriptBuilderBB bb, ScriptOption parentOption, int mode) {
		ScriptInstruction instrBuilder = parentOption.addInstruction(bb.inputData.getAdapterId(),
				TerritoriumComponent.COMPONENT_ID, mode, bb.settings.getComponentConfigId());
		return instrBuilder;
	}

	private void addViewReportTables(ScriptBuilderBB bb,ScriptOption parent) {

		for(int i =0 ; i<bb.outputDfn.getTableCount() ; i++){
			
			// create the option with an update instruction
			String tableName = bb.outputDfn.getTableAt(i).getName();
			String name = "Show " + tableName.toLowerCase().replace("_", " ").trim();
			Pair<ScriptOption, ScriptInstruction> pair = addOptionWithRunComponent(bb, parent, name, TerritoriumComponent.MODE_UPDATE_OUTPUT_TABLES_ONLY);
			pair.getA().setSynced(true);
			
			// create a single table adapter for the view table instruction
			ScriptAdapter adapter = pair.getA().addDataAdapter("Adapter for table " + tableName);
			ScriptAdapterTable tableAdapter = adapter.addEmptyTable(tableName);
			tableAdapter.setSourceTable(pair.getB().getOutputDatastoreId(), tableName);
			tableAdapter.setFetchSourceField(true);
			
			// create the view table instruction
			TableViewer tableViewer = bb.api.standardComponents().tableViewer();
			pair.getA().addInstruction(adapter.getAdapterId(), tableViewer.getId(),
					ODLComponent.MODE_DEFAULT);
			
			
		}

	}

//	private void addCopyOutput(ScriptBuilderBB bb, Pair<ScriptOption, ScriptInstruction> optionWithInstruction) {
//		optionWithInstruction.getA().addCopyTable(
//				optionWithInstruction.getB().getOutputDatastoreId(), null,
//				OutputType.COPY_ALL_TABLES, null);
//	}

	private void addCopyOutput(ScriptBuilderBB bb, Pair<ScriptOption, ScriptInstruction> optionWithInstruction) {
		int i=0;
		for(String outTableName : new String[]{"ReportCustomers","ReportClusters","ReportSolution"}){
			optionWithInstruction.getA().addCopyTable(
					optionWithInstruction.getB().getOutputDatastoreId(), bb.outputDfn.getTableAt(i).getName(),
					OutputType.COPY_TO_NEW_TABLE, outTableName);

			i++;
		}
	

	}

	private void addPointsMap(ScriptBuilderBB bb, ScriptOption parent) {
		ScriptOption option = parent.addOption("Show map", "Show map");
		option.setSynced(true);

		// always update so we get correct cluster colours (and the table is initialised)
		ScriptInstruction update=addRunComponent(bb, option, TerritoriumComponent.MODE_UPDATE_OUTPUT_TABLES_ONLY);
		ScriptAdapter mapAdapter = option.addDataAdapter("Show map input data");

		addCustomersMap(bb, update, mapAdapter);
		addClustersMap(bb, update, mapAdapter);
		
		Maps mapComponent = bb.api.standardComponents().map();
		Serializable config = null;
		try{
			config = mapComponent.getConfigClass().newInstance();
			mapComponent.setCustomTooltips(false, config);
		}catch(Exception e){
			throw new RuntimeException(e);
		}

		
		option.addInstruction(mapAdapter.getAdapterId(),mapComponent.getId(), ODLComponent.MODE_DEFAULT, config);
		option.setSynced(true);


	}

	private void addClustersMap(ScriptBuilderBB bb, ScriptInstruction updateInstruction,
			ScriptAdapter mapDataAdapter) {
		Maps maps = bb.api.standardComponents().map();
		ScriptAdapterTable clusters = mapDataAdapter.addSourcelessTable(maps.getDrawableTableDefinition());
		clusters.setTableName(PredefinedTags.DRAWABLES_INACTIVE_FOREGROUND);
		clusters.setSourceTable(updateInstruction.getOutputDatastoreId(),bb.selectedClustersTableName);
		clusters.setTableFilterFormula("\"latitude\"<>null");
		clusters.setSourceColumns(new String[][] {

			new String[] { PredefinedTags.LATITUDE, "latitude" },

			new String[] { PredefinedTags.LONGITUDE, "longitude" },

			new String[] { "colour", "display-colour"},
			

			new String[] { "legendKey", "id"},

			new String[] { "label", "id"},

		});
		
		clusters.setFormulae(new String[][] {

			new String[] { "pixelWidth", "20" },

			new String[] { "symbol","\""+ PredefinedTags.FAT_STAR +"\""},


		});
	}
	
	private void addCustomersMap(ScriptBuilderBB bb, ScriptInstruction updateInstruction,
			ScriptAdapter mapDataAdapter) {
		// setup customers to draw coloured using the cluster colour, or black if unassigned
		Maps maps = bb.api.standardComponents().map();
		ScriptAdapterTable customers = mapDataAdapter.addSourcelessTable(maps.getDrawableTableDefinition());
		customers.setSourceTable(bb.inputData.getAdapterId(), bb.inputData.getTable(0).getTableDefinition().getName());
		customers.setSourceColumns(new String[][] {

				new String[] { PredefinedTags.LATITUDE, PredefinedTags.LATITUDE },

				new String[] { PredefinedTags.LONGITUDE, PredefinedTags.LONGITUDE },


		});

		String first3LookupParamers = "\"cluster-id\",\"" + updateInstruction.getOutputDatastoreId() + ","
				+bb.selectedClustersTableName
				+ "\",\"id\"";
		String isAssignedFormula = "lookupcount(" + first3LookupParamers + ")>0";
		String legendKeyFormula = "if(" + isAssignedFormula + ",\"cluster-id\",\"Unassigned\"";
		String lookupColourFormula = "lookup(" + first3LookupParamers+",\"display-colour\")";
		lookupColourFormula = "if(" + isAssignedFormula + "," + lookupColourFormula + ",\"Black\"";
		customers.setFormulae(new String[][] {

			new String[] { "legendKey", legendKeyFormula },
			
			new String[] { "colour", lookupColourFormula },
			
			new String[] { "pixelWidth", "11" },
		});
	}
}
