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

import java.io.File;
import java.io.Serializable;

import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.Tables;
import com.opendoorlogistics.api.components.ODLComponent;
import com.opendoorlogistics.api.components.PredefinedTags;
import com.opendoorlogistics.api.scripts.ScriptAdapter;
import com.opendoorlogistics.api.scripts.ScriptAdapter.ScriptAdapterType;
import com.opendoorlogistics.api.scripts.ScriptAdapterTable.ColumnSortType;
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
import com.opendoorlogistics.api.tables.TableFlags;
import com.opendoorlogistics.territorium.utils.Pair;

public class ScriptBuilder {
	public static final String TERRITORY_ID_FIELD = "territory-id";

//	public static final String NO_CONTROLS = "No controls setup";
//	public static final String SAME_QUANTS = "same params for each cluster";
//	public static final String DIFF_QUANTS = "separate params for each cluster";
//	public static final String POINTS_OR_POLYS = "Setup for points or polygons";
//	public static final String POINTS = "Setup for points";
//	public static final String POLYS = "Setup for polygons";

	public static String customersTableName(boolean usingPolygons) {
		if (usingPolygons) {
			return "Areas";
		} else {
			return "Customers";
		}
	}

	public enum ClustererScriptType {

		// BASIC_OPTIONS_NO_INPUT_CLUSTERS(POINTS_OR_POLYS + ", " + SAME_QUANTS, false, true, false),

		// BASIC_OPTIONS_SETUP_INPUT_CLUSTERS(POINTS_OR_POLYS + ", " + DIFF_QUANTS, true, true, false),

		POINTS_SETUP_NO_INPUT_CLUSTERS("Create territories for customer points", false, true, true, false),

		POLYS_SETUP_NO_INPUT_CLUSTERS("Create territories for polygons in a shapefile", false, true, false, true),

		POINTS_SETUP_INPUT_CLUSTERS("Create territories for customer points, with territory parameters in their own table", true, true, true, false),
		POLYS_SETUP_INPUT_CLUSTERS("Create territories for polygons in a shapefile, with territory parameters in their own table", true, true, false, true),
	

		COMPONENT_ONLY_NO_INPUT_CLUSTERS("(Advanced) Add component without controls", false, false, false,
				false),

		COMPONENT_ONLY_INPUT_CLUSTERS("(Advanced) Add component without controls, with territory parameters in their own table", true, false, false, false);

		// 
		
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
		ScriptAdapter shapefileLoader;
		// ScriptAdapter internalView;
		ScriptComponentConfig settings;
		String selectedClustersTableName;
		ConfigureShapefileResult shapefile;
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
		bb.componentInputDfn = new TerritoriumComponent().getIODsDefinition(builder.getApi(),
				bb.config);
		bb.outputDfn = bb.component.getOutputDsDefinition(bb.api, ODLComponent.MODE_DEFAULT,
				bb.config);

		// create shapefile loader
		if (bb.type.polygons) {
			addShapefileLoader(builder, bb);
		}

		addInputData(builder, bb);

		// create shared stand-alone config
		addOptimiserSettings(bb, builder);
	
		if (!type.coreControls) {
			
			// just add optimiser with empty output tables
			ScriptInstruction instrBuilder = addRunComponent(bb, bb.builder,
					ODLComponent.MODE_DEFAULT);
			instrBuilder.setName("Optimise clusters");
			addCopyOutput(bb, new Pair<ScriptOption, ScriptInstruction>(bb.builder, instrBuilder));
			return;
		}

		addPrepareData(bb);

		addMapWithoutCentres(bb, bb.builder);

		addQuantitiesReport(bb, bb.builder);
		
		ScriptOption optimiser=addOptimiser(bb);

		addAnalyseOptimiserResults(bb, optimiser);

	}

	private void addOptimiserSettings(ScriptBuilderBB bb, ScriptOption option) {
		bb.settings = option.addComponentConfig("Optimiser settings", bb.component.getId(), bb.config);
	}

	private void addAnalyseOptimiserResults(ScriptBuilderBB bb, ScriptOption optimiser) {
		ScriptOption analyse = optimiser.addOption("Analyse results", "Analyse results");
		if (bb.type.points || bb.type.polygons) {
			addMapWithCentres(bb, analyse);
		}

		addViewReportTables(bb, analyse);

		addCopyOutput(bb,
				addOptionWithRunComponent(bb, analyse, "Export report tables to spreadsheet",
						TerritoriumComponent.MODE_UPDATE_OUTPUT_TABLES_ONLY));
	}

	private void addInputData(ScriptOption builder, ScriptBuilderBB bb) {
		// create the input data adapter which (for polygons) reads the shapefile loader
		bb.inputData = builder.addDataAdapter("Input data");
		ScriptInputTables inputTables = builder.getInputTables();
		for (int i = 0; i < bb.componentInputDfn.getTableCount(); i++) {
			ScriptAdapterTable sat = null;
			if (inputTables != null && i < inputTables.size()
					&& inputTables.getSourceTable(i) != null) {
				// use input table if we have one
				sat = bb.inputData.addSourcedTableToAdapter(inputTables.getSourceDatastoreId(i),
						inputTables.getSourceTable(i), bb.componentInputDfn.getTableAt(i));
			} else {
				// otherwise don't match to a source and just give a default source pointing to an identical table in
				// the spreadsheet
				sat = bb.inputData.addSourcedTableToAdapter(
						bb.api.stringConventions().getSpreadsheetAdapterId(),
						bb.componentInputDfn.getTableAt(i), bb.componentInputDfn.getTableAt(i));
			}

			if (bb.type.polygons && i == 0) {
				// set sources for long lat fields
				String lookup = "lookup(id, \"" + bb.shapefileLoader.getAdapterId()
						+ ",shapefile\", c(\"id\"), c(\"geom\"))";
				sat.setFormula(PredefinedTags.LATITUDE, "latitude(" + lookup + ")");
				sat.setFormula(PredefinedTags.LONGITUDE, "longitude(" + lookup + ")");
			}
		}
	}

	private void addShapefileLoader(ScriptOption builder, ScriptBuilderBB bb) {
		bb.shapefile = ConfigureShapefileResult.runWizard(bb.api);
		if (bb.shapefile == null) {
			throw new RuntimeException("User quit");
		}
		
		bb.shapefileLoader = builder.addDataAdapter("ShapefileLoader");
		ScriptAdapterTable table = bb.shapefileLoader.addEmptyTable("Shapefile");
		String shapefilename = bb.api.io().getAsRelativeIfWithinStandardShapefileDirectory(
				new File(bb.shapefile.getShapefile())).getPath();

		table.setSourceTable(":=Shapefile(\"" + shapefilename + "\")", "");
		table.setFetchSourceField(false);
		table.addColumn("geom", ODLColumnType.GEOM, false, "the_geom");
		// table.addColumn(PredefinedTags.ID, ODLColumnType.STRING, true,
		// "this(sp(\"ShapefileIdField)\")");
		table.addColumn(PredefinedTags.ID, ODLColumnType.STRING, false,
				bb.shapefile.getIdField());
	}

	private void addPrepareData(ScriptBuilderBB bb) {
		// add option to create input tables
	//	ScriptOption prepare = bb.builder.addOption("Prepare data", "Prepare data");
		ScriptOption parentOption = bb.builder;
		createTables(bb, parentOption);

		// add option to add/remove entries for polygons
		if (bb.type.polygons) {
			String name = "Add missing areas to table, remove invalid ones";
			ScriptOption updateAreas = parentOption.addOption(name,name);
			updateAreas.setEditorLabel(
					"Add missing areas to your table and remove those not found in the shapefile");

			// create adapter to get the missing areas and fill in default values
			ScriptAdapter add = updateAreas.addDataAdapter("AddMissing");
			ScriptAdapterTable inputTable = bb.inputData.getTable(0);
			ScriptAdapterTable missing = add.addSourcelessTable(inputTable.getTableDefinition());
			missing.setTableName("Missing");
			missing.setSourceTable("ShapefileLoader", "Shapefile");
			String lookupCountTable = bb.inputData.getAdapterId() + ","
					+ inputTable.getTableDefinition().getName();
			missing.setTableFilterFormula(
					"lookupcount(id,\"" + lookupCountTable + "\",c(\"id\"))==0");

			// remove lat longs
			for (String ll : new String[] { PredefinedTags.LATITUDE, PredefinedTags.LONGITUDE }) {
				for (int i = 0; i < missing.getColumnCount(); i++) {
					if (bb.api.stringConventions().equalStandardised(ll,
							missing.getColumnName(i))) {
						missing.removeColumn(i);
						break;
					}
				}
			}

			// set id as a source field and fill in default values
			for (int i = 0; i < missing.getColumnCount(); i++) {
				String colName = missing.getColumnName(i);
				if (bb.api.stringConventions().equalStandardised(colName, "id")) {
					missing.setSourceColumn("id", "id");
				} else {
					// try finding a default value from the component
					ODLTableDefinition dfn = bb.componentInputDfn.getTableAt(0);
					for (int j = 0; j < dfn.getColumnCount(); j++) {
						if (bb.api.stringConventions().equalStandardised(colName,
								dfn.getColumnName(j))) {
							Object defaultVal = dfn.getColumnDefaultValue(j);
							if (defaultVal != null && Number.class.isInstance(defaultVal)) {
								missing.setFormula(i, defaultVal.toString());
							} else {
								missing.setFormula(i, "");
							}
						}
					}
				}
			}

			// add a copy to the external datastore
			updateAreas.addCopyTable(add.getAdapterId(), missing.getTableDefinition().getName(),
					OutputType.APPEND_TO_EXISTING_TABLE, ScriptBuilder.customersTableName(true));

			// create adapter to get rows not in shapefile
			ScriptAdapter remove = updateAreas.addDataAdapter("FindUnidentified");
			ScriptAdapterTable toRemove = remove.addEmptyTable("ToRemove");
			toRemove.setSourceTable(bb.inputData.getAdapterId(),
					inputTable.getTableDefinition().getName());
			toRemove.setTableFilterFormula(
					"lookupcount(\"id\",\"ShapefileLoader,shapefile\",c(\"id\"))==0");

			// add delete command
			UpdateTable utcomp = bb.api.standardComponents().updateTable();
			updateAreas.addInstruction(remove.getAdapterId(), utcomp.getId(),
					ODLComponent.MODE_DEFAULT, utcomp.createConfig(true));
		}

		// add fill demo data for points
		if (bb.type.points) {
			String name = "Fill input tables with demo data";
			parentOption.addOption(name, name).addInstruction(bb.inputData.getAdapterId(),
					TerritoriumComponent.COMPONENT_ID, TerritoriumComponent.MODE_BUILD_DEMO,
					bb.settings.getComponentConfigId());
		}
	}

	private ScriptOption addOptimiser(ScriptBuilderBB bb) {
		ScriptOption optimise = bb.builder.addOption("Optimiser", "Optimiser");
	//	addOptimiserSettings(bb, optimise);
		addOptionWithRunComponent(bb, optimise, "Start optimisation", ODLComponent.MODE_DEFAULT);
		addOptionWithRunComponent(bb, optimise, "Continue optimisation",
				TerritoriumComponent.CONTINUE_OPTIMISATION_MODE);
		return optimise;
	}

	public void addParameter(ScriptOption builder, ScriptBuilderBB bb, String name,
			String initialValue) {
		Parameters parameters = bb.api.scripts().parameters();
		ScriptAdapter shapefilenameParameter = builder.addDataAdapter(name);
		shapefilenameParameter.setAdapterType(ScriptAdapterType.PARAMETER);
		ODLTableDefinition paramTableDfn = parameters.tableDefinition(false);
		ScriptAdapterTable newParameter = shapefilenameParameter.addSourcelessTable(paramTableDfn);
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
		ODLTableDefinition[] tables = createExternalTableDefinitions(bb.api,
				bb.type.createConfig());
		Serializable tableCreatorConfig = tableCreatorComponent.createConfiguration(bb.api, tables);
		ScriptOption addOption = parent.addOption(optionName, optionName);
		ScriptInstruction addInstruction = addOption.addInstruction(null,
				tableCreatorComponent.getId(), ODLComponent.MODE_DEFAULT, tableCreatorConfig);
		addOption.addCopyTable(addInstruction.getOutputDatastoreId(), "",
				OutputType.COPY_ALL_TABLES, "");

	}

	static ODLTableDefinition[] createExternalTableDefinitions(ODLApi api,
			TerritoriumConfig config) {
		ODLDatastore<? extends ODLTableDefinition> ds = new TerritoriumComponent()
				.getIODsDefinition(api, config);
		ODLTableDefinition[] tables = new ODLTableDefinition[ds.getTableCount()];
		for (int i = 0; i < tables.length; i++) {
			Tables tablesApi = api.tables();
			ODLTableDefinition original = ds.getTableAt(i);
			ODLTableDefinitionAlterable alterable = tablesApi
					.createAlterableTable(original.getName());
			tablesApi.copyTableDefinition(original, alterable);

			if (i == 0 && config.isPolygons()) {
				// remove lat long fields from the table we create in the external datastore
				for (String col : new String[] { PredefinedTags.LATITUDE,
						PredefinedTags.LONGITUDE }) {
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
		for (String outTableName : new String[] { "ReportCustomers", "ReportTerritories",
				"ReportSolution" }) {
			optionWithInstruction.getA().addCopyTable(
					optionWithInstruction.getB().getOutputDatastoreId(),
					bb.outputDfn.getTableAt(i).getName(), OutputType.REPLACE_CONTENTS_OF_EXISTING_TABLE,
					outTableName);

			i++;
		}

	}

	private static String getTableName(ScriptAdapter adapter , int i){
		return adapter.getTable(i).getTableDefinition().getName();
	}
	
	private void addQuantitiesReport(ScriptBuilderBB bb, ScriptOption parent) {
		String name = "Show quantities report";
		ScriptOption option = parent.addOption(name,name);
		option.setSynced(true);
		option.setEditorLabel("View the sum of quantities for each " + TERRITORY_ID_FIELD);
		
		// add a table to convert empty territory id to "#Unasssigned"
		ScriptAdapter adapter1 = option.addDataAdapter("MarkUnassigned");
		ScriptAdapterTable table1 = adapter1.addEmptyTable("MarkUnassigned");
		table1.setSourceTable(bb.inputData.getAdapterId(), getTableName(bb.inputData,0));
		table1.addColumn(TERRITORY_ID_FIELD, ODLColumnType.STRING, true, "if(len(\""+TERRITORY_ID_FIELD+"\")>0,\""+TERRITORY_ID_FIELD+"\",\"#Unassigned\")");
		table1.addColumn(PredefinedTags.QUANTITY, ODLColumnType.DOUBLE, false, PredefinedTags.QUANTITY);
		
		ScriptAdapter adapter2=option.addDataAdapter("QuantitiesReportData");
		ScriptAdapterTable table2=adapter2.addEmptyTable("Quantities");
		table2.setSourceTable(adapter1.getAdapterId(), getTableName(adapter1,0));
		table2.setTableFilterFormula("len(\"" + TERRITORY_ID_FIELD + "\")>0");
		
		table2.addColumn(TERRITORY_ID_FIELD, ODLColumnType.STRING, false, TERRITORY_ID_FIELD);
		table2.setColumnFlags(TERRITORY_ID_FIELD, TableFlags.FLAG_IS_GROUP_BY_FIELD);

		table2.addColumn("quantity-sum", ODLColumnType.DOUBLE, true, "groupsum(\"" +PredefinedTags.QUANTITY+"\")");

		table2.addColumn("SortField", ODLColumnType.STRING, false, TERRITORY_ID_FIELD);
		table2.setSortType(2, ColumnSortType.ASCENDING);
		
		ODLApi api = bb.api;
		option.addInstruction(adapter2.getAdapterId(), api.standardComponents().tableViewer().getId(), ODLComponent.MODE_DEFAULT);
	}
	
	private void addMapWithoutCentres(ScriptBuilderBB bb, ScriptOption parent) {
		String name = "Show map";
		ScriptOption option = parent.addOption(name, name);
		option.setSynced(true);
		ScriptAdapter mapAdapter = option.addDataAdapter(name + " adapter");
		addCustomersOrBricksMapTable(bb, null, mapAdapter);
		addMapInstruction(bb, option, mapAdapter);

	}

	private void addMapWithCentres(ScriptBuilderBB bb, ScriptOption parent) {
		ScriptOption option = parent.addOption("Show map with centres", "Show map with centres");
		option.setSynced(true);

		// always update so we get correct cluster colours (and the table is initialised)
		ScriptInstruction update = addRunComponent(bb, option,
				TerritoriumComponent.MODE_UPDATE_OUTPUT_TABLES_ONLY);
		ScriptAdapter mapAdapter = option.addDataAdapter("Show map input data with centres");

		addCustomersOrBricksMapTable(bb, update, mapAdapter);
		addClustersMap(bb, update, mapAdapter);

		addMapInstruction(bb, option, mapAdapter);

	}

	private void addMapInstruction(ScriptBuilderBB bb, ScriptOption option,
			ScriptAdapter mapAdapter) {
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

	private void addCustomersOrBricksMapTable(ScriptBuilderBB bb, ScriptInstruction updateInstruction,
			ScriptAdapter mapDataAdapter) {
		// setup customers to draw coloured using the cluster colour, or black if unassigned
		Maps maps = bb.api.standardComponents().map();
		ScriptAdapterTable customers = mapDataAdapter
				.addSourcelessTable(maps.getDrawableTableDefinition());
		customers.setSourceTable(bb.inputData.getAdapterId(),
				bb.inputData.getTable(0).getTableDefinition().getName());

		if (bb.type.points) {
			customers.setSourceColumns(new String[][] {
					new String[] { PredefinedTags.LATITUDE, PredefinedTags.LATITUDE },
					new String[] { PredefinedTags.LONGITUDE, PredefinedTags.LONGITUDE },

			});
			customers.setFormula("pixelWidth", "11");
		} else if (bb.type.polygons) {
			customers
					.setSourceColumns(new String[][] { new String[] { PredefinedTags.LATITUDE, "" },
							new String[] { PredefinedTags.LONGITUDE, "" }, });
			customers.setFormula("opaque", "0.3");
			customers.setFormula("NonOverlappingPolygonLayerGroupKey", "\"A\"");

			// Lookup geom
			String geomlookup = "lookup(" + PredefinedTags.ID + ",\""
					+ bb.shapefileLoader.getAdapterId() + ","
					+ bb.shapefileLoader.getTable(0).getTableDefinition().getName() + "\"" + ",c(\""
					+ PredefinedTags.ID + "\")" + ",c(\"geom\"))";
			customers.setFormula("geometry", geomlookup);
			customers.setTableFilterFormula(geomlookup + "!=null");
			customers.setFormula("pixelWidth", "2");
		}

		String lookupColourFormula = null;
		String isAssignedFormula = null;
		String SPCH = "\"";
		String hasId = "len(\"" + TERRITORY_ID_FIELD + "\")>0";
		String randColour  = "randColour(\"" + TERRITORY_ID_FIELD + "\")";
		if (updateInstruction != null || bb.type.inputClusters) {
			String clustersTable = "";
			if (updateInstruction != null) {
				// get from the output clusters table
				clustersTable = "\"" + updateInstruction.getOutputDatastoreId() + ","
						+ bb.selectedClustersTableName + "\"";
			} else {
				// get from the input clusters table
				clustersTable = "\"" + bb.inputData.getAdapterId() + ","
						+ bb.inputData.getTable(1).getTableDefinition().getName() + "\"";
			}

			String first3LookupParams = "\"" + TERRITORY_ID_FIELD + "\"," + clustersTable
					+ ",\"id\"";
			isAssignedFormula ="("+hasId+")"+ " && "+ "lookupcount(" + first3LookupParams + ")>0";
			lookupColourFormula = "lookup(" + first3LookupParams + ",\"display-colour\")";
		} else {
			isAssignedFormula = hasId;
			lookupColourFormula = randColour;
		}

		// We have assigned, bad id and unassigned
		String baseFormula = "if(" + isAssignedFormula + ",#OK#" + ",if("+hasId + ",#BADID#,#UNASSIGNED#))";
		String legendKeyFormula =baseFormula.replace("#OK#", SPCH+TERRITORY_ID_FIELD+SPCH);
		legendKeyFormula=legendKeyFormula.replace("#BADID#", SPCH + "(Unrecognised) "+SPCH + " & "+ SPCH + TERRITORY_ID_FIELD + SPCH );
		legendKeyFormula = legendKeyFormula.replace("#UNASSIGNED#", SPCH + "#Unassigned" + SPCH);
		
		String colourFormula = baseFormula.replace("#OK#", lookupColourFormula);
		colourFormula = colourFormula.replace("#BADID#", "lerp(colour(1,0,0)," + randColour + ",0.2)");
		colourFormula = colourFormula.replace("#UNASSIGNED#", "colour(0,0,0)");
//		String legendKeyFormula = "if(" + isAssignedFormula + ",\"" + TERRITORY_ID_FIELD
//				+ "\",\"#Unassigned\"";
//		lookupColourFormula = "if(" + isAssignedFormula + "," + lookupColourFormula + ",\"Black\"";
		customers.setFormulae(new String[][] {

				new String[] { "legendKey", legendKeyFormula },

				new String[] { "colour", colourFormula },

		});
	}


}
