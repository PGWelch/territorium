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
import com.opendoorlogistics.api.Tables;
import com.opendoorlogistics.api.components.ODLComponent;
import com.opendoorlogistics.api.components.PredefinedTags;
import com.opendoorlogistics.api.scripts.ScriptAdapter;
import com.opendoorlogistics.api.scripts.ScriptAdapter.ScriptAdapterType;
import com.opendoorlogistics.api.scripts.ScriptAdapterTable;
import com.opendoorlogistics.api.scripts.ScriptComponentConfig;
import com.opendoorlogistics.api.scripts.ScriptInputTables;
import com.opendoorlogistics.api.scripts.ScriptInstruction;
import com.opendoorlogistics.api.scripts.ScriptOption;
import com.opendoorlogistics.api.scripts.ScriptOption.OutputType;
import com.opendoorlogistics.api.scripts.parameters.Parameters;
import com.opendoorlogistics.api.scripts.parameters.Parameters.PromptType;
import com.opendoorlogistics.api.standardcomponents.Maps;
import com.opendoorlogistics.api.standardcomponents.TableCreator;
import com.opendoorlogistics.api.standardcomponents.TableViewer;
import com.opendoorlogistics.api.standardcomponents.UpdateTable;
import com.opendoorlogistics.api.tables.ODLColumnType;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLTable;
import com.opendoorlogistics.api.tables.ODLTableDefinition;
import com.opendoorlogistics.api.tables.ODLTableDefinitionAlterable;
import com.opendoorlogistics.territorium.utils.Pair;

public class ScriptBuilder {
	public static final boolean ENABLE_POLYS = true;

	public static final String NO_CONTROLS = "No controls setup";
	public static final String SAME_QUANTS = "same params for each cluster";
	public static final String DIFF_QUANTS = "separate params for each cluster";
	public static final String POINTS_OR_POLYS = "Setup for points or polygons";
	public static final String POINTS = "Setup for points";
	public static final String POLYS = "Setup for polygons";

	public static String customersTableName(boolean usingPolygons) {
		if (usingPolygons) {
			return "Areas";
		} else {
			return "Customers";
		}
	}

	public enum ClustererScriptType {
		COMPONENT_ONLY_NO_INPUT_CLUSTERS(NO_CONTROLS + ", " + SAME_QUANTS, false, false, false,
				false),

		COMPONENT_ONLY_INPUT_CLUSTERS(NO_CONTROLS + ", " + DIFF_QUANTS, true, false, false, false),

		// BASIC_OPTIONS_NO_INPUT_CLUSTERS(POINTS_OR_POLYS + ", " + SAME_QUANTS, false, true, false),

		// BASIC_OPTIONS_SETUP_INPUT_CLUSTERS(POINTS_OR_POLYS + ", " + DIFF_QUANTS, true, true, false),

		POINTS_SETUP_NO_INPUT_CLUSTERS(POINTS + ", " + SAME_QUANTS, false, true, true, false),

		POINTS_SETUP_INPUT_CLUSTERS(POINTS + ", " + DIFF_QUANTS, true, true, true, false),

		POLYS_SETUP_NO_INPUT_CLUSTERS(POLYS + ", " + SAME_QUANTS, false, true, false, true),

		POLYS_SETUP_INPUT_CLUSTERS(POLYS + ", " + DIFF_QUANTS, true, true, false, true);

		private ClustererScriptType(String description, boolean inputClusters, boolean coreControls,
				boolean points, boolean polygons) {
			this.inputClusters = inputClusters;
			this.points = points;
			this.coreControls = coreControls;
			this.description = description;
			this.polygons = polygons;
		}

		public final boolean inputClusters;
		private final boolean coreControls;
		private final boolean points;
		public final boolean polygons;
		public final String description;

		public TerritoriumConfig createConfig() {
			TerritoriumConfig config = new TerritoriumConfig();
			config.setUseInputClusterTable(inputClusters);
			config.setPolygons(polygons);
			return config;
		}
	}

	private static class ScriptBuilderBB {
		ODLApi api;
		ScriptOption builder;
		ClustererScriptType type;
		TerritoriumComponent component;
		TerritoriumConfig config;
		ODLDatastore<? extends ODLTableDefinition> componentInputDfn;
		ODLDatastore<? extends ODLTableDefinition> outputDfn;
		ScriptAdapter inputData;
		//ScriptAdapter internalView;
		ScriptComponentConfig settings;
		String selectedClustersTableName;
	}

	public void build(ClustererScriptType type, ScriptOption builder) {
		ScriptBuilderBB bb = new ScriptBuilderBB();
		bb.builder = builder;
		bb.type = type;
		bb.component = new TerritoriumComponent();
		bb.config = type.createConfig();
		bb.api = builder.getApi();
		bb.selectedClustersTableName = bb.component
				.getOutputDsDefinition(bb.api, TerritoriumComponent.MODE_DEFAULT, bb.config)
				.getTableAt(1).getName();
		bb.componentInputDfn = new TerritoriumComponent().getIODsDefinition(builder.getApi(), bb.config);
		bb.outputDfn = bb.component.getOutputDsDefinition(bb.api, ODLComponent.MODE_DEFAULT,
				bb.config);
		bb.inputData = builder.addDataAdapter("Input data");


		// create shapefile filename parameter
		if (type.polygons) {
			addParameter(builder, bb,"Shapefilename","<Enter shapefile filename here>");
			addParameter(builder, bb,"ShapefileIdField","<Enter id field from shapefile here>");
		}

		// create shapefile loader 
		if(type.polygons){
			ScriptAdapterTable table=builder.addDataAdapter("ShapefileLoader").addEmptyTable("Shapefile");
			table.setSourceTable(":=Shapefile(sp(\"Shapefilename\"))", "");
			table.setFetchSourceField(true);
			table.addColumn("geom", ODLColumnType.GEOM, false, "the_geom");
			table.addColumn("id", ODLColumnType.STRING, true,"this(\"sp(\"ShapefileIdField\")\")");
		}

		// create the input data adapter which (for polygons) reads the shapefile loader
		ScriptInputTables inputTables = builder.getInputTables();
		for (int i = 0; i < bb.componentInputDfn.getTableCount(); i++) {
			ScriptAdapterTable sat=null;
			if (inputTables != null && i < inputTables.size()
					&& inputTables.getSourceTable(i) != null) {
				// use input table if we have one
				sat=bb.inputData.addSourcedTableToAdapter(inputTables.getSourceDatastoreId(i),
						inputTables.getSourceTable(i), bb.componentInputDfn.getTableAt(i));
			} else {
				// otherwise don't match to a source and just give a default source pointing to an identical table in
				// the spreadsheet
				sat=bb.inputData.addSourcedTableToAdapter(
						bb.api.stringConventions().getSpreadsheetAdapterId(),
						bb.componentInputDfn.getTableAt(i), bb.componentInputDfn.getTableAt(i));
			}
			
			if(type.polygons && i==0){
				// set sources for long lat fields
				String lookup="lookup(id, \"ShapefileLoader,shapefile\", c(\"id\"), c(\"geom\")))";
				sat.setFormula(PredefinedTags.LATITUDE, "latitude("+lookup+")");
				sat.setFormula(PredefinedTags.LONGITUDE, "longitude("+lookup+")");
			}
		}
		
		// create shared stand-alone config
		bb.settings = builder.addComponentConfig("Settings", bb.component.getId(), bb.config);

		if (!type.coreControls) {
			// just add optimiser with empty output tables
			ScriptInstruction instrBuilder = addRunComponent(bb, bb.builder,
					ODLComponent.MODE_DEFAULT);
			instrBuilder.setName("Optimise clusters");
			addCopyOutput(bb, new Pair<ScriptOption, ScriptInstruction>(bb.builder, instrBuilder));
			return;
		}

		// add option to create input tables
		ScriptOption prepare = bb.builder.addOption("Prepare data", "Prepare data");
		createTables(bb, prepare);

		// add option to add/remove entries for polygons
		if(type.polygons){
			ScriptOption updateAreas = prepare.addOption("Update area records", "Update area records");
			updateAreas.setEditorLabel("Add missing areas to your table and remove those not found in the shapefile");
			
			// create adapter to get the missing areas and fill in default values
			ScriptAdapter add = updateAreas.addDataAdapter("AddMissing");
			ScriptAdapterTable inputTable = bb.inputData.getTable(0);
			ScriptAdapterTable missing=add.addSourcelessTable(inputTable.getTableDefinition());
			missing.setTableName("Missing");
			missing.setSourceTable("ShapefileLoader", "Shapefile");
			String lookupCountTable = bb.inputData.getAdapterId()+","+inputTable.getTableDefinition().getName();
			missing.setTableFilterFormula("lookupcount(id,\""+ lookupCountTable + "\",c(\"id\"))==0");
			for(int i =0 ; i<missing.getColumnCount();i++){
				String colName = missing.getColumnName(i);
				if(bb.api.stringConventions().equalStandardised(colName, "id")){
					missing.setSourceColumn("id", "id");
				}else{
					Object defaultVal=missing.getTableDefinition().getColumnDefaultValue(i);
					if(defaultVal!=null && Number.class.isInstance(defaultVal)){
						missing.setFormula(i, defaultVal.toString());
					}else{
						missing.setFormula(i, "");
					}
				}
			}
			
			// add a copy to the external datastore
			updateAreas.addCopyTable(add.getAdapterId(), missing.getTableDefinition().getName(),OutputType.APPEND_TO_EXISTING_TABLE, ScriptBuilder.customersTableName(true));
	
			// create adapter to get rows not in shapefile
			ScriptAdapter remove = updateAreas.addDataAdapter("FindUnidentified");
			ScriptAdapterTable toRemove = remove.addEmptyTable("ToRemove");
			toRemove.setSourceTable(bb.inputData.getAdapterId(), inputTable.getTableDefinition().getName());
			toRemove.setTableFilterFormula("lookupcount(\"id\",\"ShapefileLoader,shapefile\",c(\"id\"))==0");
			
			// add delete command
			UpdateTable utcomp=bb.api.standardComponents().updateTable();
			updateAreas.addInstruction(remove.getAdapterId(), utcomp.getId(),ODLComponent.MODE_DEFAULT, utcomp.createConfig(true));
		}
		

		// add fill demo data for points
		if (type.points) {
			String name = "Fill tables with demo data";
			prepare.addOption(name, name).addInstruction(bb.inputData.getAdapterId(),
					TerritoriumComponent.COMPONENT_ID, TerritoriumComponent.MODE_BUILD_DEMO,
					bb.settings.getComponentConfigId());
		}

		ScriptOption optimise = bb.builder.addOption("Optimise", "Optimise");
		addOptionWithRunComponent(bb, optimise, "Start optimisation", ODLComponent.MODE_DEFAULT);
		addOptionWithRunComponent(bb, optimise, "Continue optimisation",
				TerritoriumComponent.CONTINUE_OPTIMISATION_MODE);

		ScriptOption analyse = bb.builder.addOption("Analyse results", "Analyse results");
		if (type.points) {
			addPointsMap(bb, analyse);
		}
		else{
			// TODO add map for polygons....	
		}
		
		addViewReportTables(bb, analyse);

		addCopyOutput(bb,
				addOptionWithRunComponent(bb, analyse, "Export report tables to spreadsheet",
						TerritoriumComponent.MODE_UPDATE_OUTPUT_TABLES_ONLY));

	}

	
	public void addParameter(ScriptOption builder, ScriptBuilderBB bb, String name, String initialValue) {
		Parameters parameters = bb.api.scripts().parameters();
		ScriptAdapter shapefilenameParameter = builder.addDataAdapter(name);
		shapefilenameParameter.setAdapterType(ScriptAdapterType.PARAMETER);
		ODLTableDefinition paramTableDfn = parameters.tableDefinition(false);
		ScriptAdapterTable newParameter = shapefilenameParameter
				.addSourcelessTable(paramTableDfn);
		newParameter.setSourceTable("embeddedData", "");
		Tables tables = bb.api.tables();
		ODLTable dataTable = (ODLTable) bb.api.tables().copyTableDefinition(paramTableDfn,
				bb.api.tables().createAlterableDs());
		dataTable.createEmptyRow(-1);
		
		dataTable.setValueAt(PromptType.HIDDEN.name(), 0,
				tables.findColumnIndex(dataTable, Parameters.FIELDNAME_PROMPT_TYPE));
	
		dataTable.setValueAt(ODLColumnType.STRING.name(), 0,
				tables.findColumnIndex(dataTable, Parameters.FIELDNAME_VALUE_TYPE));

		dataTable.setValueAt(initialValue, 0,
				tables.findColumnIndex(dataTable, Parameters.FIELDNAME_VALUE));
		newParameter.setDataTable(dataTable);
	}

	private void createTables(ScriptBuilderBB bb, ScriptOption parent) {

		// create all empty tables
		String optionName = "Create input table(s)";
		TableCreator tableCreatorComponent = bb.api.standardComponents().tableCreator();
		ODLTableDefinition[] tables = createExternalTableDefinitions(bb.api, bb.type.createConfig());
		Serializable tableCreatorConfig = tableCreatorComponent.createConfiguration(bb.api, tables);
		ScriptOption addOption = parent.addOption(optionName, optionName);
		ScriptInstruction addInstruction = addOption.addInstruction(null,
				tableCreatorComponent.getId(), ODLComponent.MODE_DEFAULT, tableCreatorConfig);
		addOption.addCopyTable(addInstruction.getOutputDatastoreId(), "",
				OutputType.COPY_ALL_TABLES, "");

	}


	static ODLTableDefinition[] createExternalTableDefinitions(ODLApi api,
			TerritoriumConfig config) {
		ODLDatastore<? extends ODLTableDefinition> ds = new TerritoriumComponent().getIODsDefinition(api, config);
		ODLTableDefinition[] tables = new ODLTableDefinition[ds.getTableCount()];
		for (int i = 0; i < tables.length; i++) {
			Tables tablesApi = api.tables();
			ODLTableDefinition original  =ds.getTableAt(i);
			ODLTableDefinitionAlterable alterable = tablesApi.createAlterableTable(original.getName());
			tablesApi.copyTableDefinition(original, alterable);
			
			if(i==0 && config.isPolygons()){
				// remove lat long fields from the table we create in the external datastore
				for(String col: new String[]{PredefinedTags.LATITUDE,PredefinedTags.LONGITUDE}){
					alterable.deleteColumn(tablesApi.findColumnIndex(alterable, col));
				}
			}
			
			tables[i] = alterable;
		}
		return tables;
	}

	private Pair<ScriptOption, ScriptInstruction> addOptionWithRunComponent(ScriptBuilderBB bb,
			ScriptOption parentOption, String name, int mode) {
		ScriptOption optOption = parentOption.addOption(name, name);
		return new Pair<ScriptOption, ScriptInstruction>(optOption,
				addRunComponent(bb, optOption, mode));

	}

	private ScriptInstruction addRunComponent(ScriptBuilderBB bb, ScriptOption parentOption,
			int mode) {
		ScriptInstruction instrBuilder = parentOption.addInstruction(bb.inputData.getAdapterId(),
				TerritoriumComponent.COMPONENT_ID, mode, bb.settings.getComponentConfigId());
		return instrBuilder;
	}

	private void addViewReportTables(ScriptBuilderBB bb, ScriptOption parent) {

		for (int i = 0; i < bb.outputDfn.getTableCount(); i++) {

			// create the option with an update instruction
			String tableName = bb.outputDfn.getTableAt(i).getName();
			String name = "Show " + tableName.toLowerCase().replace("_", " ").trim();
			Pair<ScriptOption, ScriptInstruction> pair = addOptionWithRunComponent(bb, parent, name,
					TerritoriumComponent.MODE_UPDATE_OUTPUT_TABLES_ONLY);
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

	// private void addCopyOutput(ScriptBuilderBB bb, Pair<ScriptOption, ScriptInstruction> optionWithInstruction) {
	// optionWithInstruction.getA().addCopyTable(
	// optionWithInstruction.getB().getOutputDatastoreId(), null,
	// OutputType.COPY_ALL_TABLES, null);
	// }

	private void addCopyOutput(ScriptBuilderBB bb,
			Pair<ScriptOption, ScriptInstruction> optionWithInstruction) {
		int i = 0;
		for (String outTableName : new String[] { "ReportCustomers", "ReportClusters",
				"ReportSolution" }) {
			optionWithInstruction.getA().addCopyTable(
					optionWithInstruction.getB().getOutputDatastoreId(),
					bb.outputDfn.getTableAt(i).getName(), OutputType.COPY_TO_NEW_TABLE,
					outTableName);

			i++;
		}

	}

	private void addPointsMap(ScriptBuilderBB bb, ScriptOption parent) {
		ScriptOption option = parent.addOption("Show map", "Show map");
		option.setSynced(true);

		// always update so we get correct cluster colours (and the table is initialised)
		ScriptInstruction update = addRunComponent(bb, option,
				TerritoriumComponent.MODE_UPDATE_OUTPUT_TABLES_ONLY);
		ScriptAdapter mapAdapter = option.addDataAdapter("Show map input data");

		addCustomersPointsMap(bb, update, mapAdapter);
		addClustersMap(bb, update, mapAdapter);

		Maps mapComponent = bb.api.standardComponents().map();
		Serializable config = null;
		try {
			config = mapComponent.getConfigClass().newInstance();
			mapComponent.setCustomTooltips(false, config);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		option.addInstruction(mapAdapter.getAdapterId(), mapComponent.getId(),
				ODLComponent.MODE_DEFAULT, config);
		option.setSynced(true);

	}

	private void addClustersMap(ScriptBuilderBB bb, ScriptInstruction updateInstruction,
			ScriptAdapter mapDataAdapter) {
		Maps maps = bb.api.standardComponents().map();
		ScriptAdapterTable clusters = mapDataAdapter
				.addSourcelessTable(maps.getDrawableTableDefinition());
		clusters.setTableName(PredefinedTags.DRAWABLES_INACTIVE_FOREGROUND);
		clusters.setSourceTable(updateInstruction.getOutputDatastoreId(),
				bb.selectedClustersTableName);
		clusters.setTableFilterFormula("\"latitude\"<>null");
		clusters.setSourceColumns(new String[][] {

				new String[] { PredefinedTags.LATITUDE, "latitude" },

				new String[] { PredefinedTags.LONGITUDE, "longitude" },

				new String[] { "colour", "display-colour" },

				new String[] { "legendKey", "id" },

				new String[] { "label", "id" },

		});

		clusters.setFormulae(new String[][] {

				new String[] { "pixelWidth", "20" },

				new String[] { "symbol", "\"" + PredefinedTags.FAT_STAR + "\"" },

		});
	}

	private void addCustomersPointsMap(ScriptBuilderBB bb, ScriptInstruction updateInstruction,
			ScriptAdapter mapDataAdapter) {
		// setup customers to draw coloured using the cluster colour, or black if unassigned
		Maps maps = bb.api.standardComponents().map();
		ScriptAdapterTable customers = mapDataAdapter
				.addSourcelessTable(maps.getDrawableTableDefinition());
		customers.setSourceTable(bb.inputData.getAdapterId(),
				bb.inputData.getTable(0).getTableDefinition().getName());
		customers.setSourceColumns(new String[][] {

				new String[] { PredefinedTags.LATITUDE, PredefinedTags.LATITUDE },

				new String[] { PredefinedTags.LONGITUDE, PredefinedTags.LONGITUDE },

		});

		String first3LookupParamers = "\"cluster-id\",\"" + updateInstruction.getOutputDatastoreId()
				+ "," + bb.selectedClustersTableName + "\",\"id\"";
		String isAssignedFormula = "lookupcount(" + first3LookupParamers + ")>0";
		String legendKeyFormula = "if(" + isAssignedFormula + ",\"cluster-id\",\"Unassigned\"";
		String lookupColourFormula = "lookup(" + first3LookupParamers + ",\"display-colour\")";
		lookupColourFormula = "if(" + isAssignedFormula + "," + lookupColourFormula + ",\"Black\"";
		customers.setFormulae(new String[][] {

				new String[] { "legendKey", legendKeyFormula },

				new String[] { "colour", lookupColourFormula },

				new String[] { "pixelWidth", "11" }, });
	}
}
