package com.opendoorlogistics.territorium;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Window;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.tables.ODLTable;

public class ConfigureShapefileResult {
	private String shapefile;
	private String idField;
	
	public String getShapefile() {
		return shapefile;
	}
	public void setShapefile(String shapefile) {
		this.shapefile = shapefile;
	}
	public String getIdField() {
		return idField;
	}
	public void setIdField(String idField) {
		this.idField = idField;
	}
	
	public static ConfigureShapefileResult runWizard(ODLApi api){
		String lastVal = api.preferences().get("territorium-shapefile");
		File file = showSelectShapefile(api.app().getJFrame(), lastVal);
		if(file==null){
			return null;
		}
		api.preferences().put("territorium-shapefile", file.getAbsolutePath());
		
		// warn if large
		if(file.exists() && file.getName().toLowerCase().trim().endsWith(".shp")){
			long bytes = file.length();
			double MB= bytes/(1024.0*1024.0);
			if(MB > 25){
				if(JOptionPane.showConfirmDialog(api.app().getJFrame(), "<html>"
						+ "<p>For larger shapefiles you are advised to convert them to an .odlrg (ODL render geometry) file first.</p>"
						+ "<p>Convert using the .shp file using the shapefile exporter component in ODL Studio.</p>"
						+ "<p>You should then select the .odlrg file instead of the .shp file in this wizard.</p>"
						+ "<p>.odlrg files draw a lot quicker and use less memory than .shp files</p></html>", "Warning - large shapefile", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE)==JOptionPane.CANCEL_OPTION){
					return null;
				}

			}
		}
		
		// get example values
		int max = 3;
		ODLTable table = api.io().importShapefile(file, max);
		TreeMap<String, List<String>> map = new TreeMap<>();
		for(int i=0 ; i<table.getColumnCount(); i++){
			String colName = table.getColumnName(i);
			if(api.stringConventions().equalStandardised(colName, "the_geom")){
				continue;
			}
			List<String> vals = new ArrayList<>();
			for(int j=0;j<Math.min(max, table.getRowCount());j++){
				Object o = table.getValueAt(j, i);
				if(o!=null){
					vals.add(o.toString());
				}
			}
			map.put(colName, vals);
		}
		
		String field = showSelectFieldInShapefile(api.app().getJFrame(), map, api.preferences().get("territorium-shapefile-field"));
		if(field==null){
			return null;
		}
		api.preferences().put("territorium-shapefile-field", field);
		
		ConfigureShapefileResult result = new ConfigureShapefileResult();
		result.setShapefile(file.getAbsolutePath());
		result.setIdField(field);
		return result;
	}
	

	private static File showSelectShapefile(Component parent, String lastVal) {
		JFileChooser fc = new JFileChooser();
		fc.setDialogTitle("Select shapefile (.shp) or .odlrg file containing polygons");
		FileNameExtensionFilter filter = new FileNameExtensionFilter("Shapefile or .odlrg file",
				"shp", "odlrg");
		fc.setFileFilter(filter);
		fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
		try {
			if (lastVal != null) {
				File file = new File(lastVal);
				File dir = file.getParentFile();
				if (dir != null && dir.exists()) {
					fc.setCurrentDirectory(dir);
				}
				if (file.exists()) {
					fc.setSelectedFile(file);
				}
			}
		} catch (Exception e) {
		}

		if (fc.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
			return fc.getSelectedFile();
		}
		return null;
	}

	private static String showSelectFieldInShapefile(Component owner,
			TreeMap<String, List<String>> entriesWithExamples, String defaultVal) {
		class ReturnVal {
			String val;
		}
		ReturnVal returnVal = new ReturnVal();

		Window parentWindow = null;
		if(owner!=null){
			if(owner instanceof Window){
				parentWindow = (Window)owner;
			}else{
				parentWindow =SwingUtilities.getWindowAncestor(owner);				
			}
		}
		JDialog dialog = new JDialog(parentWindow, "Select field", Dialog.ModalityType.APPLICATION_MODAL);

		
		JList<String> list = new JList<>(
				entriesWithExamples.keySet().toArray(new String[entriesWithExamples.size()]));
		list.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
		JScrollPane listScroller = new JScrollPane(list);
		listScroller.setPreferredSize(new Dimension(200, 200));
		listScroller.setAlignmentX(JDialog.LEFT_ALIGNMENT);
		JPanel listPane = new JPanel();
		listPane.setLayout(new BoxLayout(listPane, BoxLayout.PAGE_AXIS));
		JLabel label = new JLabel("Select the field containing the postcodes/zipcodes");
		label.setLabelFor(list);
		listPane.add(label);
		listPane.add(Box.createRigidArea(new Dimension(0, 5)));
		listPane.add(listScroller);
		listPane.add(Box.createRigidArea(new Dimension(0, 10)));
		listPane.add(new JLabel("Values of selected field:"));
		listPane.add(Box.createRigidArea(new Dimension(0, 5)));
		JEditorPane previewPane = new JEditorPane();
		previewPane.setEditable(false);
		previewPane.setAlignmentX(JDialog.LEFT_ALIGNMENT);
		listPane.add(Box.createRigidArea(new Dimension(0, 5)));
		listPane.add(previewPane);
		listPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		// update the preview when the list changes
		list.addListSelectionListener(e -> {
			returnVal.val = list.getSelectedValue();
			StringBuilder preview = new StringBuilder();
			int nMax = 100;
			if (returnVal.val != null) {
				List<String> examples = entriesWithExamples.get(returnVal.val);
				if (examples != null) {
					for (String example : examples) {
						if (preview.length() > 0) {
							preview.append(", ");
						}
						preview.append(example);
						if (preview.length() > nMax) {
							break;
						}
					}
				}
			}
			previewPane.setText(preview.toString());
		});

		final JButton okButton = new JButton("OK");
		okButton.setActionCommand("Set");
		okButton.addActionListener(e -> {
			returnVal.val = list.getSelectedValue();
			dialog.setVisible(false);
		});

		dialog.getRootPane().setDefaultButton(okButton);

		JButton cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(e -> {
			returnVal.val = null;
			dialog.setVisible(false);
		});

		// Lay out the buttons from left to right.
		JPanel buttonPane = new JPanel();
		buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
		buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
		buttonPane.add(Box.createHorizontalGlue());
		buttonPane.add(okButton);
		buttonPane.add(Box.createRigidArea(new Dimension(10, 0)));
		buttonPane.add(cancelButton);

		Container contentPane = dialog.getContentPane();
		contentPane.add(listPane, BorderLayout.CENTER);
		contentPane.add(buttonPane, BorderLayout.PAGE_END);

		dialog.pack();
		if(owner!=null){
			dialog.setLocationRelativeTo(owner);
		}
		
		boolean selected = false;
		if (defaultVal != null) {
			for (int i = 0; i < list.getModel().getSize(); i++) {
				if (list.getModel().getElementAt(i).toLowerCase().trim()
						.equals(defaultVal.toLowerCase().trim())) {
					list.setSelectedIndex(i);
					selected = true;
					break;
				}
				;
			}
		}
		if (!selected && entriesWithExamples.size() > 0) {
			list.setSelectedIndex(0);
		}

		dialog.setVisible(true);
		return returnVal.val;
	}
	
}
