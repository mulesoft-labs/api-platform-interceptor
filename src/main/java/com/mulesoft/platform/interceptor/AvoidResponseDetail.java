package com.mulesoft.platform.interceptor;

public class AvoidResponseDetail extends ResponseDetail {

	public AvoidResponseDetail() {
		super(-1, "");
	}
	
	@Override
	public boolean isPassThru() {
		return true;
	}
}
