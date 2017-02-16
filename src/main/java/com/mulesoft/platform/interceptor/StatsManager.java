package com.mulesoft.platform.interceptor;

import java.util.Arrays;
import java.util.List;

import com.google.gson.JsonObject;

public class StatsManager {
	
	private enum Strategy {NONE, FAILS_VS_OKS, PORCENTAGE} ;
	private enum HttpVerb {POST,GET, NONE};
	private int statusCode;
	private String payload;
	private Strategy strategy;
	private List<String> pathIntercepted;
	private HttpVerb httpVerb;
	private int qtyToFail;
	private int failureAllowed;
	private int porcentage;
	
	public StatsManager(JsonObject jsonObject) {	
		this.statusCode = jsonObject.get("statusCode").getAsInt();
		this.payload = jsonObject.get("payload")==null?null:jsonObject.get("payload").getAsString();
		this.strategy = jsonObject.get("strategy")==null?Strategy.NONE:Strategy.valueOf(jsonObject.get("strategy").getAsString());
		String pathI=jsonObject.get("pathIntercepted")==null?null:jsonObject.get("pathIntercepted").getAsString();
		this.pathIntercepted = Arrays.asList(pathI.split(","));
		this.httpVerb = jsonObject.get("httpVerb")==null?HttpVerb.NONE:HttpVerb.valueOf(jsonObject.get("httpVerb").getAsString());
		this.failureAllowed = jsonObject.get("failureAllowed")!=null?jsonObject.get("failureAllowed").getAsInt():0;
		this.porcentage = jsonObject.get("porcentage")!=null?jsonObject.get("porcentage").getAsInt():0;
		this.qtyToFail= failureAllowed;
	}

	public ResponseDetail getResponseDetail(String path, String httpVerb) {
		if (statusCode == -1) {
			return new AvoidResponseDetail();
		}
		if(strategy == Strategy.NONE)
			return new ProcessResponseDetail(statusCode, payload, validatePath(path) && this.validateHttpVerb());
		return new ProcessResponseDetail(statusCode, payload, resolveStrategy());
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
	
	private boolean resolveStrategy(){
		if(this.strategy.equals(Strategy.FAILS_VS_OKS))
			return failVsOks();
		else if (this.strategy.equals(Strategy.PORCENTAGE))
			return porcentageFailing();
		throw new RuntimeException();
	}
	
	private boolean failVsOks(){
		if((this.qtyToFail--)>0)
			return true;
		this.qtyToFail= this.failureAllowed;
		return false;
	}
	
	private boolean porcentageFailing(){
		if(Math.random()*100>porcentage)
			return true;
		return false;
	}
}
