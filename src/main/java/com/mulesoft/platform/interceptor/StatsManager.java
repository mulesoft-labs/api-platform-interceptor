package com.mulesoft.platform.interceptor;

import java.util.Arrays;
import java.util.List;

import com.google.gson.JsonObject;

public class StatsManager {
	
	private enum Strategy {NONE, FAILS_VS_OKS, PORCENTAGE_OF_FAILING} ;
	private enum HttpMethod {POST,GET, NONE};
	private int statusCode;
	private String payload;
	private Strategy strategy;
	private List<String> pathIntercepted;
	private HttpMethod httpMethod;
	private int qtyToFail;
	private int msgBeforeFailing;
	private int porcentage;
	
	public StatsManager(JsonObject jsonObject) {	
		this.statusCode = jsonObject.get("statusCode").getAsInt();
		this.payload = jsonObject.get("payload")==null?null:jsonObject.get("payload").getAsString();
		this.strategy = jsonObject.get("failingStrategy")==null?Strategy.NONE:Strategy.valueOf(jsonObject.get("failingStrategy").getAsString());
		String pathI=jsonObject.get("pathIntercepted")==null?null:jsonObject.get("pathIntercepted").getAsString();
		this.pathIntercepted = Arrays.asList(pathI.split(","));
		this.httpMethod = jsonObject.get("httpMethod")==null?HttpMethod.NONE:HttpMethod.valueOf(jsonObject.get("httpMethod").getAsString());
		this.msgBeforeFailing = jsonObject.get("msgBeforeFailing")!=null?jsonObject.get("msgBeforeFailing").getAsInt():0;
		this.porcentage = jsonObject.get("porcentageOfFailing")!=null?jsonObject.get("porcentageOfFailing").getAsInt():0;
		this.qtyToFail= msgBeforeFailing;
	}

	public ResponseDetail getResponseDetail(String path, String httpMethod) {
		if (statusCode == -1) {
			return new AvoidResponseDetail();
		}
		if(strategy == Strategy.NONE)
			return new ProcessResponseDetail(statusCode, payload, validatePath(path) && this.validatehttpMethod());
		return new ProcessResponseDetail(statusCode, payload, resolveStrategy());
	}
	
	private boolean validatehttpMethod(){
		if(this.httpMethod.equals(HttpMethod.NONE))
			return true;
		return httpMethod.equals(this.httpMethod);
	}
	
	public boolean validatePath(String path){
		if(pathIntercepted==null)
			return true;
		for(String interceptor: pathIntercepted)
			if (path.startsWith(interceptor))
				return false;
		return true;
	}
	
	private boolean resolveStrategy(){
		if(this.strategy.equals(Strategy.FAILS_VS_OKS))
			return failVsOks();
		else if (this.strategy.equals(Strategy.PORCENTAGE_OF_FAILING))
			return porcentageFailing();
		throw new RuntimeException();
	}
	
	private boolean failVsOks(){
		if((this.qtyToFail--)>0)
			return true;
		this.qtyToFail= this.msgBeforeFailing;
		return false;
	}
	
	private boolean porcentageFailing(){
		if(Math.random()*100>porcentage)
			return true;
		return false;
	}
}
