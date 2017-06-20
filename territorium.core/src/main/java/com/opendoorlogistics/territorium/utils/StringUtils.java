/*******************************************************************************
 * Copyright (c) 2014, 2017 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.territorium.utils;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.StringWriter;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;


final public class StringUtils {
	private StringUtils(){}

	
	
//	public static int [] getCentres(Solution solution){
//		int []ret = new int[solution.getNbClusters()];
//		for(int i=0 ; i < ret.length ; i++){
//			ret[i] = solution.getClusterCentre(i);
//		}
//		return ret;
//	}
	
	public static void setOneLineLogging(){
        System.setProperty("java.util.logging.SimpleFormatter.format",
                "%1$tH:%1$tM:%1$tS.%1$tL %4$-7s %5$s %6$s%n");
	
	}
	

	
	public static String toPrettyPrintJSON(Object object) {
		ObjectMapper mapper = new ObjectMapper();
		mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		// disable this to get data times etc out in  ISO 8601 format (which is close to a standard)
		mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);		

		mapper.enable(SerializationFeature.INDENT_OUTPUT);					
		StringWriter writer = new StringWriter();
		try {
			mapper.writeValue(writer, object);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return writer.toString();
	}

	public static void setClipboard(String s){
		StringSelection stringSelection = new StringSelection(s);
		Clipboard clpbrd = Toolkit.getDefaultToolkit().getSystemClipboard();
		clpbrd.setContents(stringSelection, null);
		
	}
}
