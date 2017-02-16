package com.mulesoft.platform.interceptor;

public class ProcessResponseDetail extends ResponseDetail {
	
	private boolean response=false;
	
	public ProcessResponseDetail(int statusCode, String message) {
		super(statusCode, message);
	}
	
	public ProcessResponseDetail(int statusCode, String message, boolean response) {
		super(statusCode, message);
		this.response= response;
	}
	
	@Override
	public boolean isPassThru() {
		return response;
	}
}
