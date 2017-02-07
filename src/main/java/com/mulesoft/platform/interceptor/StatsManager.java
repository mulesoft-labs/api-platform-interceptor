package com.mulesoft.platform.interceptor;

import com.google.gson.JsonObject;

public class StatsManager {
	
	final int statusCode;
	//final String strategy;
	
	public StatsManager(JsonObject jsonObject) {	
		this.statusCode = jsonObject.get("statusCode").getAsInt();
	}

	public ResponseDetail getResponseDetail(String path, String httpVerb) {
		if (statusCode == -1) {
			return new AvoidResponseDetail();
		}
		return new ProcessResponseDetail(statusCode, "Generic message");
	}
}
