package com.mulesoft.platform.interceptor;

public class ProcessResponseDetail extends ResponseDetail {
	
	public ProcessResponseDetail(int statusCode, String message) {
		super(statusCode, message);
	}
	
	@Override
	public boolean isPassThru() {
		return false;
	}

}
