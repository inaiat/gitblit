/*
 * Copyright 2011 gitblit.com.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gitblit.wicket;


/**
 * Simple subclass of mixed parameter url coding strategy that works around the
 * encoded forward-slash issue that is present in some servlet containers.
 * 
 * https://issues.apache.org/jira/browse/WICKET-1303
 * http://tomcat.apache.org/security-6.html
 * 
 * @author James Moger
 * 
 */
//TODO Wicket 6
// Delete this class
public class GitblitParamUrlCodingStrategy {}
//public class GitblitParamUrlCodingStrategy extends MixedParamUrlCodingStrategy {
//
//	private Logger logger = LoggerFactory.getLogger(GitblitParamUrlCodingStrategy.class);
//
//	/**
//	 * Construct.
//	 * 
//	 * @param <C>
//	 * @param mountPath
//	 *            mount path (not empty)
//	 * @param bookmarkablePageClass
//	 *            class of mounted page (not null)
//	 * @param parameterNames
//	 *            the parameter names (not null)
//	 */
//	public <C extends Page> GitblitParamUrlCodingStrategy(String mountPath,
//			Class<C> bookmarkablePageClass, String[] parameterNames) {
//		super(mountPath, bookmarkablePageClass, parameterNames);
//	}
//
//	/**
//	 * Url encodes a string that is mean for a URL path (e.g., between slashes)
//	 * 
//	 * @param string
//	 *            string to be encoded
//	 * @return encoded string
//	 */
//	protected String urlEncodePathComponent(String string) {
//		char altChar = GitBlit.getChar(Keys.web.forwardSlashCharacter, '/');
//		if (altChar != '/') {
//			string = string.replace('/', altChar);
//		}
//		return super.urlEncodePathComponent(string);
//	}
//
//	/**
//	 * Returns a decoded value of the given value (taken from a URL path
//	 * section)
//	 * 
//	 * @param value
//	 * @return Decodes the value
//	 */
//	protected String urlDecodePathComponent(String value) {
//		char altChar = GitBlit.getChar(Keys.web.forwardSlashCharacter, '/');
//		if (altChar != '/') {
//			value = value.replace(altChar, '/');
//		}
//		return super.urlDecodePathComponent(value);
//	}
//
//	/**
//	 * Gets the decoded request target.
//	 * 
//	 * @param requestParameters
//	 *            the request parameters
//	 * @return the decoded request target
//	 */
//	@Override
//	public IRequestTarget decode(RequestParameters requestParameters) {
//		final String parametersFragment = requestParameters.getPath().substring(
//				getMountPath().length());
//		logger.debug(MessageFormat
//				.format("REQ: {0} PARAMS {1}", getMountPath(), parametersFragment));
//
//		final PageParameters parameters = new PageParameters(decodeParameters(parametersFragment,
//				requestParameters.getParameters()));
//		return super.decode(requestParameters);
//	}
//}