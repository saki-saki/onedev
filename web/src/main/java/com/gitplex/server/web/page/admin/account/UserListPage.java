package com.gitplex.server.web.page.admin.account;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PageableListView;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.gitplex.server.GitPlex;
import com.gitplex.server.manager.AccountManager;
import com.gitplex.server.model.Account;
import com.gitplex.server.security.SecurityUtils;
import com.gitplex.server.web.WebConstants;
import com.gitplex.server.web.behavior.OnTypingDoneBehavior;
import com.gitplex.server.web.component.avatar.AvatarLink;
import com.gitplex.server.web.component.confirmdelete.ConfirmDeleteAccountModal;
import com.gitplex.server.web.component.link.AccountLink;
import com.gitplex.server.web.page.account.AccountPage;
import com.gitplex.server.web.page.account.setting.ProfileEditPage;
import com.gitplex.server.web.page.admin.AdministrationPage;

import de.agilecoders.wicket.core.markup.html.bootstrap.navigation.BootstrapPagingNavigator;
import de.agilecoders.wicket.core.markup.html.bootstrap.navigation.ajax.BootstrapAjaxPagingNavigator;

@SuppressWarnings("serial")
public class UserListPage extends AdministrationPage {

	private PageableListView<Account> accountsView;
	
	private BootstrapPagingNavigator pagingNavigator;
	
	private WebMarkupContainer accountsContainer; 
	
	private WebMarkupContainer noAccountsContainer;
	
	private String searchInput;
	
	@Override
	protected void onInitialize() {
		super.onInitialize();
		
		TextField<String> searchField;
		add(searchField = new TextField<String>("searchAccounts", Model.of("")));
		searchField.add(new OnTypingDoneBehavior(100) {

			@Override
			protected void onTypingDone(AjaxRequestTarget target) {
				searchInput = searchField.getInput();
				target.add(accountsContainer);
				target.add(pagingNavigator);
				target.add(noAccountsContainer);
			}

		});
		
		add(new Link<Void>("addNew") {

			@Override
			protected void onConfigure() {
				super.onConfigure();
				setVisible(SecurityUtils.canManageSystem());
			}

			@Override
			public void onClick() {
				setResponsePage(NewUserPage.class);
			}
			
		});
		
		accountsContainer = new WebMarkupContainer("accounts") {

			@Override
			protected void onConfigure() {
				super.onConfigure();
				setVisible(!accountsView.getModelObject().isEmpty());
			}
			
		};
		accountsContainer.setOutputMarkupPlaceholderTag(true);
		add(accountsContainer);
		
		accountsContainer.add(accountsView = new PageableListView<Account>("accounts", new LoadableDetachableModel<List<Account>>() {

			@Override
			protected List<Account> load() {
				List<Account> accounts = new ArrayList<>();
				for (Account account: GitPlex.getInstance(AccountManager.class).findAll()) {
					if (account.matches(searchInput) && !account.isOrganization()) {
						accounts.add(account);
					}
				}
				accounts.sort((account1, account2) -> account1.getDisplayName().compareTo(account2.getDisplayName()));
				return accounts;
			}
			
		}, WebConstants.DEFAULT_PAGE_SIZE) {

			@Override
			protected void populateItem(ListItem<Account> item) {
				Account account = item.getModelObject();

				item.add(new AvatarLink("avatarLink", item.getModelObject(), null));
				item.add(new AccountLink("nameLink", item.getModelObject()));
				item.add(new Label("email", account.getEmail()));
				
				item.add(new Link<Void>("setting") {

					@Override
					public void onClick() {
						PageParameters params = AccountPage.paramsOf(item.getModelObject());
						setResponsePage(ProfileEditPage.class, params);
					}

				});
				
				Long accountId = account.getId();
				item.add(new AjaxLink<Void>("delete") {

					@Override
					public void onClick(AjaxRequestTarget target) {
						Account account = GitPlex.getInstance(AccountManager.class).load(accountId);
						if (!account.getDepots().isEmpty()) {
							target.appendJavaScript("alert('Please delete or transfer repositories under this account first');");
						} else {
							new ConfirmDeleteAccountModal(target) {
								
								@Override
								protected void onDeleted(AjaxRequestTarget target) {
									target.add(accountsContainer);
									target.add(pagingNavigator);
									target.add(noAccountsContainer);
								}
								
								@Override
								protected Account getAccount() {
									return GitPlex.getInstance(AccountManager.class).load(accountId);
								}
							};
						}
					}

					@Override
					protected void onConfigure() {
						super.onConfigure();
						
						Account account = item.getModelObject();
						setVisible(SecurityUtils.canManage(account) && !account.equals(getLoginUser()));
					}

				});
			}
			
		});

		add(pagingNavigator = new BootstrapAjaxPagingNavigator("accountsPageNav", accountsView) {

			@Override
			protected void onConfigure() {
				super.onConfigure();
				setVisible(accountsView.getPageCount() > 1);
			}
			
		});
		pagingNavigator.setOutputMarkupPlaceholderTag(true);
		
		add(noAccountsContainer = new WebMarkupContainer("noAccounts") {

			@Override
			protected void onConfigure() {
				super.onConfigure();
				setVisible(accountsView.getModelObject().isEmpty());
			}
			
		});
		noAccountsContainer.setOutputMarkupPlaceholderTag(true);
	}

}