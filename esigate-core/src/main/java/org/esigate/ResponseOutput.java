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

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map.Entry;
import java.util.Set;

import org.esigate.api.HttpResponse;
import org.esigate.output.Output;

/**
 * Output implementation that simply writes to an HttpServletResponse.
 * 
 * @author Francois-Xavier Bonnet
 * 
 */
class ResponseOutput extends Output {
	private final HttpResponse response;
	private OutputStream outputStream;

	public ResponseOutput(HttpResponse response) {
		this.response = response;
	}

	/** {@inheritDoc} 
	 * @throws IOException */
	@Override
	public void open() throws IOException {
		response.setStatus(getStatusCode());
		copyHeaders();
		outputStream = new ResponseOutputStream(response.getOutputStream());
	}

	/** {@inheritDoc} */
	@Override
	public OutputStream getOutputStream() {
		return outputStream;
	}

	/**
	 * Copy all the headers to the response
	 */
	private void copyHeaders() {
		for (Entry<String, Set<String>> entry : getHeaders().entrySet()) {
			Set<String> values = entry.getValue();
			for (String value : values) {
				response.addHeader(entry.getKey(), value);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @throws IOException
	 */
	@Override
	public void close() throws IOException {
		if (outputStream != null) {
			outputStream.close();
		}
	}
}
