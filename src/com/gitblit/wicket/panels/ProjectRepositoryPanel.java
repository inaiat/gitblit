/*
 * Copyright 2012 gitblit.com.
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
package com.gitblit.wicket.panels;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.wicket.Component;
import org.apache.wicket.Localizer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.gitblit.Constants.AccessRestrictionType;
import com.gitblit.GitBlit;
import com.gitblit.Keys;
import com.gitblit.SyndicationServlet;
import com.gitblit.models.RepositoryModel;
import com.gitblit.models.UserModel;
import com.gitblit.utils.ArrayUtils;
import com.gitblit.utils.StringUtils;
import com.gitblit.wicket.GitBlitWebSession;
import com.gitblit.wicket.WicketUtils;
import com.gitblit.wicket.pages.BasePage;
import com.gitblit.wicket.pages.DocsPage;
import com.gitblit.wicket.pages.EditRepositoryPage;
import com.gitblit.wicket.pages.LogPage;
import com.gitblit.wicket.pages.SummaryPage;
import com.gitblit.wicket.pages.TicketsPage;
import com.gitblit.wicket.pages.TreePage;

public class ProjectRepositoryPanel extends BasePanel {

	private static final long serialVersionUID = 1L;

	public ProjectRepositoryPanel(String wicketId, Localizer localizer, Component parent,
			final boolean isAdmin, final RepositoryModel entry,
			final Map<AccessRestrictionType, String> accessRestrictions) {
		super(wicketId);

		final boolean showSwatch = GitBlit.getBoolean(Keys.web.repositoryListSwatches, true);
		final boolean gitServlet = GitBlit.getBoolean(Keys.git.enableGitServlet, true);
		final boolean showSize = GitBlit.getBoolean(Keys.web.showRepositorySizes, true);

		// repository swatch
		Component swatch;
		if (entry.isBare) {
			swatch = new Label("repositorySwatch", "&nbsp;").setEscapeModelStrings(false);
		} else {
			swatch = new Label("repositorySwatch", "!");
			WicketUtils.setHtmlTooltip(swatch, localizer.getString("gb.workingCopyWarning", parent));
		}
		WicketUtils.setCssBackground(swatch, entry.toString());
		add(swatch);
		swatch.setVisible(showSwatch);

		PageParameters pp = WicketUtils.newRepositoryParameter(entry.name);
		add(new LinkPanel("repositoryName", "list", StringUtils.getRelativePath(entry.projectPath,
				StringUtils.stripDotGit(entry.name)), SummaryPage.class, pp));
		add(new Label("repositoryDescription", entry.description).setVisible(!StringUtils
				.isEmpty(entry.description)));

		if (StringUtils.isEmpty(entry.originRepository)) {
			add(new Label("originRepository").setVisible(false));
		} else {
			Fragment forkFrag = new Fragment("originRepository", "originFragment", this);
			forkFrag.add(new LinkPanel("originRepository", null, StringUtils.stripDotGit(entry.originRepository), 
					SummaryPage.class, WicketUtils.newRepositoryParameter(entry.originRepository)));
			add(forkFrag);
		}

		add(new BookmarkablePageLink<Void>("tickets", TicketsPage.class, pp).setVisible(entry.useTickets));
		add(new BookmarkablePageLink<Void>("docs", DocsPage.class, pp).setVisible(entry.useDocs));

		if (entry.isFrozen) {
			add(WicketUtils.newImage("frozenIcon", "cold_16x16.png", localizer.getString("gb.isFrozen", parent)));
		} else {
			add(WicketUtils.newClearPixel("frozenIcon").setVisible(false));
		}

		if (entry.isFederated) {
			add(WicketUtils.newImage("federatedIcon", "federated_16x16.png", localizer.getString("gb.isFederated", parent)));
		} else {
			add(WicketUtils.newClearPixel("federatedIcon").setVisible(false));
		}
		switch (entry.accessRestriction) {
		case NONE:
			add(WicketUtils.newBlankImage("accessRestrictionIcon").setVisible(false));
			break;
		case PUSH:
			add(WicketUtils.newImage("accessRestrictionIcon", "lock_go_16x16.png",
					accessRestrictions.get(entry.accessRestriction)));
			break;
		case CLONE:
			add(WicketUtils.newImage("accessRestrictionIcon", "lock_pull_16x16.png",
					accessRestrictions.get(entry.accessRestriction)));
			break;
		case VIEW:
			add(WicketUtils.newImage("accessRestrictionIcon", "shield_16x16.png",
					accessRestrictions.get(entry.accessRestriction)));
			break;
		default:
			add(WicketUtils.newBlankImage("accessRestrictionIcon"));
		}

		if (StringUtils.isEmpty(entry.owner)) {
			add(new Label("repositoryOwner").setVisible(false));
		} else {
			UserModel ownerModel = GitBlit.self().getUserModel(entry.owner);
			String owner = entry.owner;
			if (ownerModel != null) {
				owner = ownerModel.getDisplayName();
			}
			add(new Label("repositoryOwner", owner + " (" +
					localizer.getString("gb.owner", parent) + ")"));
		}

		UserModel user = GitBlitWebSession.get().getUser();
		if (user == null) {
			user = UserModel.ANONYMOUS;
		}
		Fragment repositoryLinks;
		boolean showOwner = entry.isOwner(user.username);
		// owner of personal repository gets admin powers
		boolean showAdmin = isAdmin || entry.isUsersPersonalRepository(user.username);

		if (showAdmin || showOwner) {
			repositoryLinks = new Fragment("repositoryLinks", showAdmin ? "repositoryAdminLinks"
					: "repositoryOwnerLinks", this);
			repositoryLinks.add(new BookmarkablePageLink<Void>("editRepository", EditRepositoryPage.class,
					WicketUtils.newRepositoryParameter(entry.name)));
			if (showAdmin) {
				
				AjaxLink<Void> deleteLink = new ConfirmationLink<Void>("deleteRepository",MessageFormat.format(
						localizer.getString("gb.deleteRepository", parent), entry)) {
					private static final long serialVersionUID = 1L;

					@Override
					public void onClick(AjaxRequestTarget target) {
						if (GitBlit.self().deleteRepositoryModel(entry)) {
							// redirect to the owning page
							if (entry.isPersonalRepository()) {
								setResponsePage(getPage().getClass(), WicketUtils.newUsernameParameter(entry.projectPath.substring(1)));
							} else {
								setResponsePage(getPage().getClass(), WicketUtils.newProjectParameter(entry.projectPath));
							}
						} else {
							error(MessageFormat.format(getString("gb.repositoryDeleteFailed"), entry));
						}						
					}
				};
				repositoryLinks.add(deleteLink);
			}
		} else {
			repositoryLinks = new Fragment("repositoryLinks", "repositoryUserLinks", this);
		}

		repositoryLinks.add(new BookmarkablePageLink<Void>("tree", TreePage.class, WicketUtils
				.newRepositoryParameter(entry.name)).setEnabled(entry.hasCommits));

		repositoryLinks.add(new BookmarkablePageLink<Void>("log", LogPage.class, WicketUtils
				.newRepositoryParameter(entry.name)).setEnabled(entry.hasCommits));

		add(repositoryLinks);

		String lastChange;
		if (entry.lastChange.getTime() == 0) {
			lastChange = "--";
		} else {
			lastChange = getTimeUtils().timeAgo(entry.lastChange);
		}
		Label lastChangeLabel = new Label("repositoryLastChange", lastChange);
		add(lastChangeLabel);
		WicketUtils.setCssClass(lastChangeLabel, getTimeUtils().timeAgoCss(entry.lastChange));

		if (entry.hasCommits) {
			// Existing repository
			add(new Label("repositorySize", entry.size).setVisible(showSize));
		} else {
			// New repository
			add(new Label("repositorySize", localizer.getString("gb.empty", parent)).setEscapeModelStrings(false));
		}

		add(new ExternalLink("syndication", SyndicationServlet.asLink("", entry.name, null, 0)));

		List<String> repositoryUrls = new ArrayList<String>();
		if (gitServlet) {
			// add the Gitblit repository url
			repositoryUrls.add(BasePage.getRepositoryUrl(entry));
		}
		repositoryUrls.addAll(GitBlit.self().getOtherCloneUrls(entry.name));

		String primaryUrl = ArrayUtils.isEmpty(repositoryUrls) ? "" : repositoryUrls.remove(0);
		add(new RepositoryUrlPanel("repositoryCloneUrl", primaryUrl));
	}
}
