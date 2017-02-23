/*
 * (c) 2003-2017 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */

package com.mulesoft.platform.interceptor;

import static com.mulesoft.platform.interceptor.Interceptor.NEW_LINE;

import java.util.Arrays;
import java.util.List;

import com.google.gson.JsonObject;

public class StatsManager {
	private enum Strategy {
		NONE, FAILS_VS_OKS, PERCENTAGE_OF_FAILING
	};

	private enum HttpMethod {
		POST, GET, NONE
	};

	private int statusCode;
	private String payload;
	private Strategy strategy;
	private List<String> pathIntercepted;
	private HttpMethod httpMethod;
	private int msgBeforeFailing;
	private int porcentage;
	
	private int qtyToFail;
	
	private static final String STATUS_CODE_KEY = "statusCode";
	private static final String PAYLOAD_KEY = "payload";
	private static final String FAIL_STRATEGY_KEY = "failingStrategy";
	private static final String PATH_TO_INTERCEPT_KEY = "pathIntercepted";
	private static final String HTTP_METHOD_TO_FILTER_KEY = "httpMethod";
	private static final String MSG_BEFORE_FAILING_KEY = "msgBeforeFailing";
	private static final String PERCENTAGE_OF_FAILING_KEY = "porcentageOfFailing";

	// TODO: Accept several different criteria instead of only one.
	
	public StatsManager(JsonObject jsonObject) {
		statusCode = jsonObject.get(STATUS_CODE_KEY).getAsInt();
		payload = jsonObject.get(PAYLOAD_KEY) == null ? null : jsonObject.get(PAYLOAD_KEY).getAsString();
		strategy = jsonObject.get(FAIL_STRATEGY_KEY) == null ? Strategy.NONE : Strategy.valueOf(jsonObject.get(FAIL_STRATEGY_KEY).getAsString().toUpperCase());
		String pathToIntercept = jsonObject.get(PATH_TO_INTERCEPT_KEY) == null ? null : jsonObject.get(PATH_TO_INTERCEPT_KEY).getAsString();
		pathIntercepted = Arrays.asList(pathToIntercept.split(","));
		httpMethod = jsonObject.get(HTTP_METHOD_TO_FILTER_KEY) == null ? HttpMethod.NONE : HttpMethod.valueOf(jsonObject.get(HTTP_METHOD_TO_FILTER_KEY).getAsString().toUpperCase());
		msgBeforeFailing = jsonObject.get(MSG_BEFORE_FAILING_KEY) != null ? jsonObject.get(MSG_BEFORE_FAILING_KEY).getAsInt() : -1;
		porcentage = jsonObject.get(PERCENTAGE_OF_FAILING_KEY) != null	? jsonObject.get(PERCENTAGE_OF_FAILING_KEY).getAsInt() : -1;
		qtyToFail = msgBeforeFailing;
	}

	public FilterResponse getFilterResponse(String path, String httpMethod) {
		if (statusCode == -1) {
			return new FilterResponsePassThru();
		}
		if (strategy == Strategy.NONE) {
			return new FilterResponseWithData(statusCode, payload, isPathPassThru(path) && isHttpMethodPassThru());
		}
		return new FilterResponseWithData(statusCode, payload, resolveStrategy());
	}

	private boolean isHttpMethodPassThru() {
		switch(httpMethod) {
			case NONE:
				return true;
			default:
				return httpMethod.equals(this.httpMethod);
		}		
	}

	public boolean isPathPassThru(String path) {
		if (pathIntercepted == null) {
			return true;
		}
		for (String interceptor : pathIntercepted) {
			if (path.startsWith(interceptor)) {
				return false;
			}
		}
		return true;
	}

	private boolean resolveStrategy() {
		switch(strategy) {
			case FAILS_VS_OKS:
				return failVsOks();
			case PERCENTAGE_OF_FAILING:
				return porcentageFailing();
			default:
				throw new RuntimeException();
		}
	}

	private boolean failVsOks() {
		if ((qtyToFail--) > 0) {
			return true;
		}
		qtyToFail = msgBeforeFailing;
		return false;
	}

	private boolean porcentageFailing() {
		return Math.random() * 100 > porcentage;
	}
	
	@Override
	public String toString() {
		final StringBuilder stringBuilder = new StringBuilder();
		stringBuilder
			.append(NEW_LINE)
			.append("Criteria")
			.append(NEW_LINE)
			.append("Status code to be returned: ")
			.append(statusCode)
			.append(NEW_LINE)
			.append("Payload to be returned: ")
			.append(payload)
			.append(NEW_LINE)
			.append("Fail strategy used: ")
			.append(strategy)
		    .append(NEW_LINE);
		if (!pathIntercepted.isEmpty()) {
			stringBuilder.append("Path(s): ");
			for (String path : pathIntercepted) {
				stringBuilder.append(path).append(" ");
			}
		}
		stringBuilder
			.append(NEW_LINE)
			.append("HTTP method: ")
			.append(httpMethod.name())
			.append(NEW_LINE)
			.append("Messages before failing: ")
			.append(msgBeforeFailing)
			.append(NEW_LINE)
			.append("Percentage: ")
			.append(porcentage);
		return stringBuilder.toString();
	}
}