package com.jl.crm.android;

import android.content.*;
import android.location.LocationManager;
import com.jl.crm.android.activities.CrmWebOAuthActivity;
import com.jl.crm.client.*;
import dagger.*;
import org.springframework.security.crypto.encrypt.*;
import org.springframework.social.connect.sqlite.SQLiteConnectionRepository;
import org.springframework.social.connect.sqlite.support.SQLiteConnectionRepositoryHelper;
import org.springframework.social.connect.support.ConnectionFactoryRegistry;
import org.springframework.social.oauth2.AccessGrant;

import javax.inject.Singleton;

import static android.content.Context.LOCATION_SERVICE;

@Module (library = true, injects = {CrmWebOAuthActivity.class})
public class CrmModule {
	private Crm application;

	public CrmModule(Crm crm) {
		this.application = crm;
	}

	@Provides
	@Singleton
	LocationManager provideLocationManager() {
		return (LocationManager) application.getSystemService(LOCATION_SERVICE);
	}

	@Provides
	@Singleton
	SharedPreferences sharedPreferences(@InjectAndroidApplicationContext Context context) {
		String sharedPrefsName = context.getString(R.string.shared_preferences);
		return context.getSharedPreferences(sharedPrefsName, Context.MODE_PRIVATE);
	}

	@Provides
	CrmOperations crmOperations (CrmConnectionFactory connectionFactory , SharedPreferences preferences ){
		final String accessToken = preferences.getString( "accessToken",null );
		AccessGrant accessGrant = new AccessGrant(accessToken);
		return connectionFactory.createConnection(accessGrant).getApi();
	}
	/*@Provides
	@Singleton
	CrmOperations crmOperations(final CrmConnectionFactory connectionFactory, final SharedPreferences preferences) {
		AsyncTask<?, ?, CrmOperations> crmOperationsAsyncTask = new AsyncTask<Object, Object, CrmOperations>() {
			@Override
			protected CrmOperations doInBackground(Object... params) {
				final String accessToken = preferences.getString("accessToken", null);
				AccessGrant accessGrant = new AccessGrant(accessToken);
				return connectionFactory.createConnection(accessGrant).getApi();
			}
		};

		try {
			return crmOperationsAsyncTask.execute().get();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}

	}*/

	@Provides
	@Singleton
	SQLiteConnectionRepository sqLiteConnectionRepository(
			                                                       SQLiteConnectionRepositoryHelper sqLiteConnectionRepositoryHelper,
			                                                       ConnectionFactoryRegistry connectionFactoryRegistry) {
		TextEncryptor textEncryptor = AndroidEncryptors.text("password", "5c0744940b5c369b");
		return new SQLiteConnectionRepository(sqLiteConnectionRepositoryHelper, connectionFactoryRegistry, textEncryptor);
	}

	@Provides
	@Singleton
	ConnectionFactoryRegistry connectionFactoryRegistry() {
		return new ConnectionFactoryRegistry();
	}

	@Provides
	@Singleton
	SQLiteConnectionRepositoryHelper repositoryHelper(@InjectAndroidApplicationContext Context context) {
		return new SQLiteConnectionRepositoryHelper(context);
	}

	@Provides
	@Singleton
	CrmApiAdapter apiAdapter() {
		return new CrmApiAdapter();
	}

	private String fullUrl(String baseUrl, String end) {
		String base = !baseUrl.endsWith("/") ? baseUrl + "/" : baseUrl;
		String newEnd = end.startsWith("/") ? end.substring(1) : end;
		return base + newEnd;
	}

	@Provides
	@Singleton
	CrmServiceProvider serviceProvider(@InjectAndroidApplicationContext Context c) {
		String baseUrl = c.getString(R.string.base_uri);
		String clientId = c.getString(R.string.oauth_client_id);
		String clientSecret = c.getString(R.string.oauth_client_secret);
		String accessTokenUri = fullUrl(baseUrl, c.getString(R.string.oauth_token_uri));
		String authorizeUri = fullUrl(baseUrl, c.getString(R.string.oauth_authorize_uri));
		return new CrmServiceProvider(baseUrl, clientId, clientSecret, authorizeUri, accessTokenUri);
	}

	@Provides
	@Singleton
	CrmConnectionFactory crmConnectionFactory(ConnectionFactoryRegistry connectionFactoryRegistry,
			                                           CrmServiceProvider crmServiceProvider,
			                                           CrmApiAdapter crmApiAdapter) {
		CrmConnectionFactory crmConnectionFactory = new CrmConnectionFactory(crmServiceProvider, crmApiAdapter);
		connectionFactoryRegistry.addConnectionFactory(crmConnectionFactory);
		return crmConnectionFactory;
	}

	@Provides
	@Singleton
	@InjectAndroidApplicationContext
	Context provideApplicationContext() {
		return this.application.getApplicationContext();
	}
}