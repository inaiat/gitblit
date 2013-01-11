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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.apache.wicket.Application;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.protocol.http.servlet.ServletWebRequest;
import org.apache.wicket.request.Url;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.flow.RedirectToUrlException;
import org.apache.wicket.request.http.WebResponse;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gitblit.Constants;
import com.gitblit.Constants.AccessPermission;
import com.gitblit.Constants.AccessRestrictionType;
import com.gitblit.Constants.AuthorizationControl;
import com.gitblit.Constants.FederationStrategy;
import com.gitblit.GitBlit;
import com.gitblit.Keys;
import com.gitblit.models.ProjectModel;
import com.gitblit.models.RepositoryModel;
import com.gitblit.models.TeamModel;
import com.gitblit.models.UserModel;
import com.gitblit.utils.StringUtils;
import com.gitblit.utils.TimeUtils;
import com.gitblit.wicket.GitBlitWebSession;
import com.gitblit.wicket.WicketUtils;
import com.gitblit.wicket.panels.LinkPanel;

public abstract class BasePage extends WebPage {
	
	private static final long serialVersionUID = -8118978869813114548L;

	private ResourceReference BOOTSTRAP_RESPONSIVE = new CssResourceReference(BasePage.class, "bootstrap/css/bootstrap-responsive.css");

	private final Logger logger;
	
	private transient TimeUtils timeUtils;

	public BasePage() {
		super();
		logger = LoggerFactory.getLogger(getClass());
		customizeHeader();
		login();
	}

	public BasePage(PageParameters params) {
		super(params);
		logger = LoggerFactory.getLogger(getClass());
		customizeHeader();
		login();
	}
	
	private void customizeHeader() {
		if (GitBlit.getBoolean(Keys.web.useResponsiveLayout, true)) {
			add(new Behavior() {
				private static final long serialVersionUID = 1L;

				@Override
				public void renderHead(Component component, IHeaderResponse response) {
					super.renderHead(component, response);
					response.render(CssHeaderItem.forReference(BOOTSTRAP_RESPONSIVE));
				}
			});
			
			
		}
	}
	
	protected String getLanguageCode() {
		return GitBlitWebSession.get().getLocale().getLanguage();
	}
	
	protected String getCountryCode() {
		return GitBlitWebSession.get().getLocale().getCountry().toLowerCase();
	}
	
	protected TimeUtils getTimeUtils() {
		if (timeUtils == null) {
			ResourceBundle bundle;		
			try {
				bundle = ResourceBundle.getBundle("com.gitblit.wicket.GitBlitWebApp", GitBlitWebSession.get().getLocale());
			} catch (Throwable t) {
				bundle = ResourceBundle.getBundle("com.gitblit.wicket.GitBlitWebApp");
			}
			timeUtils = new TimeUtils(bundle);
		}
		return timeUtils;
	}
	
	@Override
	protected void onBeforeRender() {
		if (GitBlit.isDebugMode()) {
			// strip Wicket tags in debug mode for jQuery DOM traversal
			Application.get().getMarkupSettings().setStripWicketTags(true);
		}
		super.onBeforeRender();
	}

	@Override
	protected void onAfterRender() {
		if (GitBlit.isDebugMode()) {
			// restore Wicket debug tags
			Application.get().getMarkupSettings().setStripWicketTags(false);
		}
		super.onAfterRender();
	}	

	private void login() {
		GitBlitWebSession session = GitBlitWebSession.get();
		if (session.isLoggedIn() && !session.isSessionInvalidated()) {
			// already have a session
			return;
		}
		
		// try to authenticate by servlet request
		HttpServletRequest httpRequest = (HttpServletRequest) getRequest().getContainerRequest();
		UserModel user = GitBlit.self().authenticate(httpRequest);

		// Login the user
		if (user != null) {
			// issue 62: fix session fixation vulnerability
			session.replaceSession();
			session.setUser(user);

			// Set Cookie
			WebResponse response = (WebResponse) getRequestCycle().getResponse();
			GitBlit.self().setCookie(response, user);
			
			session.continueRequest();
		}
	}

	protected void setupPage(String repositoryName, String pageName) {
		if (repositoryName != null && repositoryName.trim().length() > 0) {
			add(new Label("title", getServerName() + " - " + repositoryName));
		} else {
			add(new Label("title", getServerName()));
		}

		ExternalLink rootLink = new ExternalLink("rootLink", urlFor(RepositoriesPage.class, null).toString());
		WicketUtils.setHtmlTooltip(rootLink, GitBlit.getString(Keys.web.siteName, Constants.NAME));
		add(rootLink);

		// Feedback panel for info, warning, and non-fatal error messages
		add(new FeedbackPanel("feedback"));

		// footer
		if (GitBlit.getBoolean(Keys.web.authenticateViewPages, true)
				|| GitBlit.getBoolean(Keys.web.authenticateAdminPages, true)) {
			UserFragment userFragment = new UserFragment("userPanel", "userFragment", BasePage.this);
			add(userFragment);
		} else {
			add(new Label("userPanel", ""));
		}

		add(new Label("gbVersion", "v" + Constants.VERSION));
		if (GitBlit.getBoolean(Keys.web.aggressiveHeapManagement, false)) {
			System.gc();
		}
	}

	protected Map<AccessRestrictionType, String> getAccessRestrictions() {
		Map<AccessRestrictionType, String> map = new LinkedHashMap<AccessRestrictionType, String>();
		for (AccessRestrictionType type : AccessRestrictionType.values()) {
			switch (type) {
			case NONE:
				map.put(type, getString("gb.notRestricted"));
				break;
			case PUSH:
				map.put(type, getString("gb.pushRestricted"));
				break;
			case CLONE:
				map.put(type, getString("gb.cloneRestricted"));
				break;
			case VIEW:
				map.put(type, getString("gb.viewRestricted"));
				break;
			}
		}
		return map;
	}
	
	protected Map<AccessPermission, String> getAccessPermissions() {
		Map<AccessPermission, String> map = new LinkedHashMap<AccessPermission, String>();
		for (AccessPermission type : AccessPermission.values()) {
			switch (type) {
			case NONE:
				map.put(type, MessageFormat.format(getString("gb.noPermission"), type.code));
				break;
			case EXCLUDE:
				map.put(type, MessageFormat.format(getString("gb.excludePermission"), type.code));
				break;
			case VIEW:
				map.put(type, MessageFormat.format(getString("gb.viewPermission"), type.code));
				break;
			case CLONE:
				map.put(type, MessageFormat.format(getString("gb.clonePermission"), type.code));
				break;
			case PUSH:
				map.put(type, MessageFormat.format(getString("gb.pushPermission"), type.code));
				break;
			case CREATE:
				map.put(type, MessageFormat.format(getString("gb.createPermission"), type.code));
				break;
			case DELETE:
				map.put(type, MessageFormat.format(getString("gb.deletePermission"), type.code));
				break;
			case REWIND:
				map.put(type, MessageFormat.format(getString("gb.rewindPermission"), type.code));
				break;
			}
		}
		return map;
	}
	
	protected Map<FederationStrategy, String> getFederationTypes() {
		Map<FederationStrategy, String> map = new LinkedHashMap<FederationStrategy, String>();
		for (FederationStrategy type : FederationStrategy.values()) {
			switch (type) {
			case EXCLUDE:
				map.put(type, getString("gb.excludeFromFederation"));
				break;
			case FEDERATE_THIS:
				map.put(type, getString("gb.federateThis"));
				break;
			case FEDERATE_ORIGIN:
				map.put(type, getString("gb.federateOrigin"));
				break;
			}
		}
		return map;
	}
	
	protected Map<AuthorizationControl, String> getAuthorizationControls() {
		Map<AuthorizationControl, String> map = new LinkedHashMap<AuthorizationControl, String>();
		for (AuthorizationControl type : AuthorizationControl.values()) {
			switch (type) {
			case AUTHENTICATED:
				map.put(type, getString("gb.allowAuthenticatedDescription"));
				break;
			case NAMED:
				map.put(type, getString("gb.allowNamedDescription"));
				break;
			}
		}
		return map;
	}

	protected TimeZone getTimeZone() {
		return GitBlit.getBoolean(Keys.web.useClientTimezone, false) ? GitBlitWebSession.get()
				.getTimezone() : GitBlit.getTimezone();
	}

	protected String getServerName() {
		ServletWebRequest servletWebRequest = (ServletWebRequest) getRequest();
		HttpServletRequest req = servletWebRequest.getContainerRequest();
		return req.getServerName();
	}
	
	public static String getRepositoryUrl(RepositoryModel repository) {
		StringBuilder sb = new StringBuilder();
		sb.append(WicketUtils.getGitblitURL(RequestCycle.get().getRequest()));
		sb.append(Constants.GIT_PATH);
		sb.append(repository.name);
		
		// inject username into repository url if authentication is required
		if (repository.accessRestriction.exceeds(AccessRestrictionType.NONE)
				&& GitBlitWebSession.get().isLoggedIn()) {
			String username = GitBlitWebSession.get().getUsername();
			sb.insert(sb.indexOf("://") + 3, username + "@");
		}
		return sb.toString();
	}
	
	protected List<ProjectModel> getProjectModels() {
		final UserModel user = GitBlitWebSession.get().getUser();
		List<ProjectModel> projects = GitBlit.self().getProjectModels(user, true);
		return projects;
	}
	
	protected List<ProjectModel> getProjects(PageParameters params) {
		if (params == null) {
			return getProjectModels();
		}

		boolean hasParameter = false;
		String regex = WicketUtils.getRegEx(params);
		String team = WicketUtils.getTeam(params);		
		int daysBack = params.get("db").toInt(0);

		List<ProjectModel> availableModels = getProjectModels();
		Set<ProjectModel> models = new HashSet<ProjectModel>();

		if (!StringUtils.isEmpty(regex)) {
			// filter the projects by the regex
			hasParameter = true;
			Pattern pattern = Pattern.compile(regex);
			for (ProjectModel model : availableModels) {
				if (pattern.matcher(model.name).find()) {
					models.add(model);
				}
			}
		}

		if (!StringUtils.isEmpty(team)) {
			// filter the projects by the specified teams
			hasParameter = true;
			List<String> teams = StringUtils.getStringsFromValue(team, ",");

			// need TeamModels first
			List<TeamModel> teamModels = new ArrayList<TeamModel>();
			for (String name : teams) {
				TeamModel teamModel = GitBlit.self().getTeamModel(name);
				if (teamModel != null) {
					teamModels.add(teamModel);
				}
			}

			// brute-force our way through finding the matching models
			for (ProjectModel projectModel : availableModels) {
				for (String repositoryName : projectModel.repositories) {
					for (TeamModel teamModel : teamModels) {
						if (teamModel.hasRepositoryPermission(repositoryName)) {
							models.add(projectModel);
						}
					}
				}
			}
		}

		if (!hasParameter) {
			models.addAll(availableModels);
		}

		// time-filter the list
		if (daysBack > 0) {
			Calendar cal = Calendar.getInstance();
			cal.set(Calendar.HOUR_OF_DAY, 0);
			cal.set(Calendar.MINUTE, 0);
			cal.set(Calendar.SECOND, 0);
			cal.set(Calendar.MILLISECOND, 0);
			cal.add(Calendar.DATE, -1 * daysBack);
			Date threshold = cal.getTime();
			Set<ProjectModel> timeFiltered = new HashSet<ProjectModel>();
			for (ProjectModel model : models) {
				if (model.lastChange.after(threshold)) {
					timeFiltered.add(model);
				}
			}
			models = timeFiltered;
		}

		List<ProjectModel> list = new ArrayList<ProjectModel>(models);
		Collections.sort(list);
		return list;
	}

	public void warn(String message, Throwable t) {
		logger.warn(message, t);
	}
	
	public void error(String message, boolean redirect) {
		logger.error(message  + " for " + GitBlitWebSession.get().getUsername());
		if (redirect) {
			GitBlitWebSession.get().cacheErrorMessage(message);
//			String relativeUrl = urlFor(RepositoriesPage.class, null).toString();
//			String absoluteUrl = RequestUtils.toAbsolutePath(relativeUrl);			
			//TODO Wicket 6. Verify abstoluteUrl
			String absoluteUrl = RequestCycle.get().getUrlRenderer().renderFullUrl(
					   Url.parse(urlFor(RepositoriesPage.class,null).toString()));			
			throw new RedirectToUrlException(absoluteUrl);
		} else {
			super.error(message);
		}
	}

	public void error(String message, Throwable t, boolean redirect) {
		logger.error(message, t);
		if (redirect) {
			GitBlitWebSession.get().cacheErrorMessage(message);
			throw new RestartResponseException(getApplication().getHomePage());
		} else {
			super.error(message);
		}
	}

	public void authenticationError(String message) {
		logger.error(getRequest().getUrl() + " for " + GitBlitWebSession.get().getUsername());
		if (!GitBlitWebSession.get().isLoggedIn()) {
			// cache the request if we have not authenticated.
			// the request will continue after authentication.
			GitBlitWebSession.get().cacheRequest(getClass());
		}
		error(message, true);
	}

	/**
	 * Panel fragment for displaying login or logout/change_password links.
	 * 
	 */
	static class UserFragment extends Fragment {

		private static final long serialVersionUID = 1L;

		public UserFragment(String id, String markupId, MarkupContainer markupProvider) {
			super(id, markupId, markupProvider);

			GitBlitWebSession session = GitBlitWebSession.get();
			if (session.isLoggedIn()) {				
				UserModel user = session.getUser();
				boolean editCredentials = GitBlit.self().supportsCredentialChanges();
				boolean standardLogin = session.authenticationType.isStandard();

				// username, logout, and change password
				add(new Label("username", user.getDisplayName() + ":"));
				add(new LinkPanel("loginLink", null, markupProvider.getString("gb.logout"),
						LogoutPage.class).setVisible(standardLogin));
				
				// quick and dirty hack for showing a separator
				add(new Label("separator", "|").setVisible(standardLogin && editCredentials));
				add(new BookmarkablePageLink<Void>("changePasswordLink", 
						ChangePasswordPage.class).setVisible(editCredentials));
			} else {
				// login
				add(new Label("username").setVisible(false));
				add(new Label("loginLink").setVisible(false));
				add(new Label("separator").setVisible(false));
				add(new Label("changePasswordLink").setVisible(false));
			}
		}
	}
}
