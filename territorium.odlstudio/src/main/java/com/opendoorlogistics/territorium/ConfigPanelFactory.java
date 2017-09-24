/*******************************************************************************
 * Copyright (c) 2014, 2017 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.territorium;

import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.components.ComponentConfigurationEditorAPI;
import com.opendoorlogistics.api.ui.UIFactory;
import com.opendoorlogistics.api.ui.UIFactory.DoubleChangedListener;
import com.opendoorlogistics.api.ui.UIFactory.IntChangedListener;

public class ConfigPanelFactory {

	static JPanel create(final TerritoriumConfig conf, ComponentConfigurationEditorAPI factory,
			boolean isFixedIO) {
		UIFactory uiFactory = factory.getApi().uiFactory();
		JPanel panel = uiFactory.createVerticalLayoutPanel();

		class Helper {
			JPanel nbClusters;
			JPanel minQuantity;
			JPanel maxQuantity;

			void addLine(JComponent component) {
				panel.add(component);
			}

			void updateEnabled() {
				nbClusters.setEnabled(conf.isUseInputClusterTable() == false);
				minQuantity.setEnabled(conf.isUseInputClusterTable() == false);
				maxQuantity.setEnabled(conf.isUseInputClusterTable() == false);
			}
		}
		Helper helper = new Helper();

		JPanel distances = factory.getApi().uiFactory().createDistancesEditor(conf.getDistancesConfig(),
				 UIFactory.EDIT_OUTPUT_UNITS);
		distances.setBorder(BorderFactory.createTitledBorder("Distances"));
		panel.add(distances);
		helper.addLine(distances);

		JPanel optPanel = new JPanel();
		optPanel.setLayout(new GridLayout(4, 2, 10, 2));
		optPanel.setBorder(BorderFactory.createTitledBorder("Clusterer options"));

		if (!isFixedIO) {
			final JCheckBox checkBox = new JCheckBox("Specify cluster quantities in table?");
			checkBox.setSelected(conf.isUseInputClusterTable());
			checkBox.addChangeListener(new ChangeListener() {

				@Override
				public void stateChanged(ChangeEvent e) {
					conf.setUseInputClusterTable(checkBox.isSelected());
					helper.updateEnabled();
				}
			});
			optPanel.add(checkBox);
			// optPanel.addWhitespace(6);
		}

		helper.nbClusters = uiFactory.createIntegerEntryPane("Number of clusters", conf.getNumberClusters(),
				"Set the number of clusters you want to create", new IntChangedListener() {

					@Override
					public void intChange(int newInt) {
						conf.setNumberClusters(newInt);
					}

				});
		optPanel.add(helper.nbClusters);
		
		optPanel.add(new JPanel()); // dummy panel to skip to next line


		helper.minQuantity = uiFactory.createDoubleEntryPane("Cluster min quantity", conf.getMinClusterQuantity(),
				"Set the min quantity of each cluster", new DoubleChangedListener() {

					@Override
					public void doubleChange(double newDbl) {
						conf.setMinClusterQuantity(newDbl);
					}
				});
		optPanel.add(helper.minQuantity);
		
		
		helper.maxQuantity = uiFactory.createDoubleEntryPane("Cluster max quantity", conf.getMaxClusterQuantity(),
				"Set the max quantity of each cluster", new DoubleChangedListener() {

					@Override
					public void doubleChange(double newDbl) {
						conf.setMaxClusterQuantity(newDbl);
					}
				});
		optPanel.add(helper.maxQuantity);
		

		JPanel maxSecs = uiFactory.createIntegerEntryPane("Max. run seconds", conf.getMaxSecondsOptimization(),
				"Maximum number of seconds to optimise for. Disable this option by setting to -1.",
				new IntChangedListener() {

					@Override
					public void intChange(int newInt) {
						conf.setMaxSecondsOptimization(newInt);
					}

				});

		JPanel maxSteps = uiFactory.createIntegerEntryPane("Max. run steps", conf.getMaxStepsOptimization(),
				"Maximum number of steps to optimise for. Disable this option by setting to -1.",
				new IntChangedListener() {

					@Override
					public void intChange(int newInt) {
						conf.setMaxStepsOptimization(newInt);
					}

				});

		final JCheckBox useSwapsCheck = new JCheckBox("Use swap moves", conf.isUseSwapMoves());
		useSwapsCheck.setToolTipText("Swap moves are slow but can sometimes improve the solution quality.");
		useSwapsCheck.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				conf.setUseSwapMoves(useSwapsCheck.isSelected());
			}
		});
		optPanel.add(maxSecs);
		optPanel.add(maxSteps);
		optPanel.add(useSwapsCheck);
		helper.addLine(optPanel);

		helper.updateEnabled();

		return panel;
	}

}
