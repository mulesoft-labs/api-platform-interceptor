package com.mulesoft.platform.interceptor;

public abstract class ResponseDetail {

	final int statusCode;
	final String message;
	
	public ResponseDetail(int statusCode, String message) {
		this.statusCode = statusCode;
		this.message = message;
	}
	
	public int getStatusCode() {
		return statusCode;
	}
	
	public String getMessage() {
		return message;
	}
	
	public abstract boolean isPassThru();
}