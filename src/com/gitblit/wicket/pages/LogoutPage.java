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
package com.gitblit.wicket.pages;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.request.http.WebResponse;

import com.gitblit.GitBlit;
import com.gitblit.models.UserModel;
import com.gitblit.wicket.GitBlitWebSession;

public class LogoutPage extends WebPage {

	public LogoutPage() {
		GitBlitWebSession session = GitBlitWebSession.get();
		UserModel user = session.getUser();
		GitBlit.self().setCookie((WebResponse) getResponse(), null);
		GitBlit.self().logout(user);
		session.invalidate();		
		//TODO Wicket 6 setRedirect(false);
		//setRedirect(true);
		setResponsePage(getApplication().getHomePage());
	}
}