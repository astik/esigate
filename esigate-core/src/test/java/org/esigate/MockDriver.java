/* 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.esigate;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.esigate.events.EventManager;
import org.esigate.http.GenericHttpRequest;
import org.esigate.test.http.HttpResponseBuilder;

public class MockDriver implements RequestExecutor {
	private Driver driver;

	public static class MockDriverBuilder implements RequestExecutorBuilder {
		private MockDriver mockDriver = new MockDriver();

		@Override
		public RequestExecutorBuilder setEventManager(EventManager eventManager) {
			return this;
		}

		@Override
		public RequestExecutorBuilder setDriver(Driver driver) {
			mockDriver.driver = driver;
			return this;
		}

		@Override
		public RequestExecutorBuilder setProperties(Properties properties) {
			return this;
		}

		public void addResource(String relUrl, String content) {
			mockDriver.addResource(relUrl, content);
		}

		@Override
		public RequestExecutor build() {
			return mockDriver;
		}

	}

	private final HashMap<String, String> resources = new HashMap<String, String>();

	private MockDriver() {
	}

	private final static Properties getDefaultProperties() {
		Properties defaultProperties = new Properties();
		defaultProperties.put(Parameters.REMOTE_URL_BASE.name, "http://localhost");
		return defaultProperties;
	}

	public void addResource(String relUrl, String content) {
		if (relUrl.startsWith("http://") || relUrl.startsWith("http://"))
			resources.put(relUrl, content);
		else {
			String url = "http://localhost";
			if (!relUrl.isEmpty() && !relUrl.startsWith("/"))
				url = url + "/";
			url = url + relUrl;
			resources.put(url, content);
		}
	}

	protected HttpResponse getResource(String url) throws HttpErrorPage {
		String result = resources.get(url);

		if (result == null) {
			throw new HttpErrorPage(404, "Not found", "The page: " + url + " does not exist");
		}
		try {
			return new HttpResponseBuilder().status(200).reason("OK").entity(result).build();
		} catch (UnsupportedEncodingException e) {
			throw new HttpErrorPage(500, e.toString(), e.toString());
		}
	}

	@Override
	public HttpResponse createAndExecuteRequest(HttpEntityEnclosingRequest request, String url, boolean b) throws HttpErrorPage {
		return getResource(url);
	}

	@Override
	public HttpResponse executeSingleRequest(GenericHttpRequest httpRequest) {
		try {
			return getResource(httpRequest.getRequestLine().getUri());
		} catch (HttpErrorPage e) {
			return e.getHttpResponse();
		}
	}

	public static MockDriver createMockDriver(String name) {
		return (MockDriver) createDriver(name).getRequestExecutor();
	}

	public static MockDriver createMockDriver(String name, Properties defaultProps) {
		return (MockDriver) createDriver(name, defaultProps).getRequestExecutor();
	}

	public static MockDriver createMockDriver() {
		return (MockDriver) createDriver().getRequestExecutor();
	}

	public static Driver createDriver() {
		return createDriver("mock");
	}

	public static Driver createDriver(String name) {
		return createDriver(name, getDefaultProperties());
	}

	public void initHttpRequestParams(HttpRequest request, Map<String, String> parameters) throws HttpErrorPage {
		driver.initHttpRequestParams(request, parameters);
	}

	public static Driver createDriver(String name, Properties defaultProps) {
		Driver driver = Driver.builder().setName(name).setProperties(defaultProps).setRequestExecutorBuilder(new MockDriverBuilder()).build();
		DriverFactory.put(name, driver);
		return driver;
	}

}
