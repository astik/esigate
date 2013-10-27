package org.esigate.servlet.impl;

import java.io.IOException;
import java.net.URI;
import java.util.Properties;

import javax.servlet.ServletException;

import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.esigate.ConfigurationException;
import org.esigate.Driver;
import org.esigate.HttpErrorPage;
import org.esigate.RequestExecutor;
import org.esigate.events.EventManager;
import org.esigate.http.GenericHttpRequest;
import org.esigate.http.IOExceptionHandler;
import org.esigate.servlet.HttpServletMediator;
import org.esigate.servlet.ResponseCapturingWrapper;
import org.esigate.util.HttpRequestHelper;
import org.esigate.util.UriUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpServletDriver implements RequestExecutor {
	private Driver driver;

	private final static class HttpServletDriverBuilder implements RequestExecutorBuilder {
		private HttpServletDriver httpServletDriver = new HttpServletDriver();

		@Override
		public RequestExecutorBuilder setEventManager(EventManager eventManager) {
			return this;
		}

		@Override
		public RequestExecutorBuilder setDriver(Driver driver) {
			httpServletDriver.driver = driver;
			return this;
		}

		@Override
		public RequestExecutorBuilder setProperties(Properties properties) {
			return this;
		}

		@Override
		public RequestExecutor build() {
			if (httpServletDriver.driver == null)
				throw new ConfigurationException("driver is mandatory");
			return httpServletDriver;
		}

	}

	public static RequestExecutorBuilder builder() {
		return new HttpServletDriverBuilder();
	}

	private final static Logger LOG = LoggerFactory.getLogger(HttpServletDriver.class);

	private HttpServletDriver() {
	}

	@Override
	public HttpResponse createAndExecuteRequest(HttpEntityEnclosingRequest request, String url, boolean proxy) throws HttpErrorPage {
		HttpServletMediator mediator = (HttpServletMediator) HttpRequestHelper.getMediator(request);
		ResponseCapturingWrapper wrappedResponse = new ResponseCapturingWrapper(mediator.getResponse(), driver);
		try {
			if (proxy)
				mediator.getFilterChain().doFilter(mediator.getRequest(), wrappedResponse);
			else
				mediator.getRequest().getRequestDispatcher(getRelUrl(request, url)).forward(mediator.getRequest(), wrappedResponse);
		} catch (IOException e) {
			throw new HttpErrorPage(HttpStatus.SC_BAD_GATEWAY, e.getMessage(), e);
		} catch (ServletException e) {
			throw new HttpErrorPage(HttpStatus.SC_BAD_GATEWAY, e.getMessage(), e);
		}
		HttpResponse result = wrappedResponse.getResponse();
		if (result.getStatusLine().getStatusCode() >= 400)
			throw new HttpErrorPage(result);
		return result;
	}

	@Override
	public HttpResponse executeSingleRequest(GenericHttpRequest request) {
		HttpServletMediator mediator = (HttpServletMediator) HttpRequestHelper.getMediator(request);
		ResponseCapturingWrapper wrappedResponse = new ResponseCapturingWrapper(mediator.getResponse(), driver);
		try {
			mediator.getRequest().getRequestDispatcher(request.getRequestLine().getUri()).include(mediator.getRequest(), wrappedResponse);
		} catch (IOException e) {
			int statusCode = HttpStatus.SC_INTERNAL_SERVER_ERROR;
			String statusText = "Error retrieving URL";
			LOG.warn(request.getRequestLine() + " -> " + statusCode + " " + statusText);
			return IOExceptionHandler.toHttpResponse(e);
		} catch (ServletException e) {
			int statusCode = HttpStatus.SC_INTERNAL_SERVER_ERROR;
			String statusText = "Error retrieving URL";
			LOG.warn(request.getRequestLine() + " -> " + statusCode + " " + statusText);
			return new BasicHttpResponse(new BasicStatusLine(HttpVersion.HTTP_1_1, statusCode, statusText));
		}
		return wrappedResponse.getResponse();
	}

	private String getRelUrl(HttpEntityEnclosingRequest request, String url) {
		URI uri = UriUtils.createUri(url);
		String relUrl = uri.getPath();
		HttpServletMediator mediator = (HttpServletMediator) HttpRequestHelper.getMediator(request);
		relUrl = relUrl.substring(mediator.getRequest().getContextPath().length());
		if (uri.getRawQuery() != null)
			relUrl += "?" + uri.getRawQuery();
		return relUrl;
	}

}
