/*
 * (c) 2003-2017 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */

package com.mulesoft.platform.interceptor;

public class FilterResponseWithData extends FilterResponse {

	private boolean isPassThru = false;

	public FilterResponseWithData(int statusCode, String message) {
		super(statusCode, message);
	}

	public FilterResponseWithData(int statusCode, String message, boolean isPassThru) {
		super(statusCode, message);
		this.isPassThru = isPassThru;
	}

	@Override
	public boolean isPassThru() {
		return isPassThru;
	}
}
