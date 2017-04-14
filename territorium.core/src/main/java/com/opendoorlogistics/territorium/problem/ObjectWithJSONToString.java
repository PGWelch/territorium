package com.opendoorlogistics.territorium.problem;

import com.opendoorlogistics.territorium.utils.StringUtils;

public class ObjectWithJSONToString {
	@Override
	public String toString(){
		return StringUtils.toPrettyPrintJSON(this);
	}
}
