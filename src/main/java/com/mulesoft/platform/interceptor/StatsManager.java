package com.mulesoft.platform.interceptor;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonObject;

public class StatsManager {
	
	private enum Strategy {NONE} ;
	private enum HttpVerb {POST,GET, NONE};
	private int statusCode;
	private String payload;
	private Strategy strategy;
	private Map<Integer, Integer> history;
	private List<String> pathIntercepted;
	private HttpVerb httpVerb;
	//final String strategy;
	
	public StatsManager(JsonObject jsonObject) {	
		this.statusCode = jsonObject.get("statusCode").getAsInt();
		this.payload = jsonObject.get("payload")==null?null:jsonObject.get("payload").getAsString();
		this.strategy = jsonObject.get("strategy")==null?Strategy.NONE:Strategy.valueOf(jsonObject.get("strategy").getAsString());
		String pathI=jsonObject.get("pathIntercepted")==null?null:jsonObject.get("pathIntercepted").getAsString();
		this.pathIntercepted = Arrays.asList(pathI.split(","));
		this.httpVerb = jsonObject.get("httpVerb")==null?HttpVerb.NONE:HttpVerb.valueOf(jsonObject.get("httpVerb").getAsString());
	}

	public ResponseDetail getResponseDetail(String path, String httpVerb) {
		if (statusCode == -1) {
			return new AvoidResponseDetail();
		}
		if(strategy == Strategy.NONE)
			return new ProcessResponseDetail(statusCode, payload, validatePath(path) && this.validateHttpVerb());
		return resolveStrategy();
	}
	
	private boolean validateHttpVerb(){
		if(this.httpVerb.equals(HttpVerb.NONE))
			return true;
		return httpVerb.equals(this.httpVerb);
	}
	
	public boolean validatePath(String path){
		if(pathIntercepted==null)
			return true;
		for(String interceptor: pathIntercepted)
			if (path.startsWith(interceptor))
				return false;
		return true;
	}
	
	private ResponseDetail resolveStrategy(){
//		Math m = Math.;
		return null;
	}
}
