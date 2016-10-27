package com.bg.haitao.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ResultConvertor {
	static private ObjectMapper mapper = new ObjectMapper();
	
	final static String defaultErrMsg = "{\"status\":\"666\", \"msg\":\"all by yourself, dude\"}";
	
	static public String toJson(Object obj) {
		try {
			return mapper.writeValueAsString(obj);
		} catch (JsonProcessingException e) {
			return defaultErrMsg;
		}
	}
}
