/*
 * Copyright (c) 2015 Ha Duy Trung
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

package io.github.hidroh.materialistic;

import android.accounts.AccountManager;
import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.net.HttpCookie;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import io.github.hidroh.materialistic.accounts.UserServices;
import io.github.hidroh.materialistic.accounts.UserServicesClient;
import io.github.hidroh.materialistic.appwidget.WidgetConfigActivity;
import io.github.hidroh.materialistic.data.AlgoliaClient;
import io.github.hidroh.materialistic.data.AlgoliaPopularClient;
import io.github.hidroh.materialistic.data.FavoriteManager;
import io.github.hidroh.materialistic.data.FeedbackClient;
import io.github.hidroh.materialistic.data.HackerNewsClient;
import io.github.hidroh.materialistic.data.ItemManager;
import io.github.hidroh.materialistic.data.ItemSyncService;
import io.github.hidroh.materialistic.data.ReadabilityClient;
import io.github.hidroh.materialistic.data.RestServiceFactory;
import io.github.hidroh.materialistic.data.SessionManager;
import io.github.hidroh.materialistic.data.UserManager;
import io.github.hidroh.materialistic.appwidget.WidgetService;
import io.github.hidroh.materialistic.widget.FavoriteRecyclerViewAdapter;
import io.github.hidroh.materialistic.widget.MultiPageItemRecyclerViewAdapter;
import io.github.hidroh.materialistic.widget.PopupMenu;
import io.github.hidroh.materialistic.widget.SinglePageItemRecyclerViewAdapter;
import io.github.hidroh.materialistic.widget.StoryRecyclerViewAdapter;
import io.github.hidroh.materialistic.widget.SubmissionRecyclerViewAdapter;
import io.github.hidroh.materialistic.widget.ThreadPreviewRecyclerViewAdapter;
import okhttp3.Cache;
import okhttp3.CacheControl;
import okhttp3.Call;
import okhttp3.Cookie;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;

@Module(
        injects = {
                SettingsActivity.class,
                AboutActivity.class,
                AskActivity.class,
                FavoriteActivity.class,
                FeedbackActivity.class,
                ItemActivity.class,
                JobsActivity.class,
                ListActivity.class,
                BestActivity.class,
                NewActivity.class,
                SearchActivity.class,
                ShowActivity.class,
                PopularActivity.class,
                LoginActivity.class,
                ComposeActivity.class,
                SubmitActivity.class,
                UserActivity.class,
                ThreadPreviewActivity.class,
                OfflineWebActivity.class,
                WidgetConfigActivity.class,
                FavoriteFragment.class,
                ItemFragment.class,
                ListFragment.class,
                WebFragment.class,
                ReadabilityFragment.class,
                ReleaseNotesActivity.class,
                StoryRecyclerViewAdapter.class,
                FavoriteRecyclerViewAdapter.class,
                SinglePageItemRecyclerViewAdapter.class,
                MultiPageItemRecyclerViewAdapter.class,
                SubmissionRecyclerViewAdapter.class,
                ThreadPreviewRecyclerViewAdapter.class,
                ItemSyncService.class,
                WidgetService.class
        },
        library = true
)
public class ActivityModule {
    public static final String ALGOLIA = "algolia";
    public static final String POPULAR = "popular";
    public static final String HN = "hn";
    private static final String TAG_OK_HTTP = "OkHttp";
    private static final long CACHE_SIZE = 20 * 1024 * 1024; // 20 MB

    private final Context mContext;

    public ActivityModule(Context context) {
        mContext = context;
    }

    @Provides @Singleton
    public Context provideContext() {
        return mContext;
    }

    @Provides @Singleton @Named(HN)
    public ItemManager provideHackerNewsClient(HackerNewsClient client) {
        return client;
    }

    @Provides @Singleton @Named(ALGOLIA)
    public ItemManager provideAlgoliaClient(AlgoliaClient client) {
        return client;
    }

    @Provides @Singleton @Named(POPULAR)
    public ItemManager provideAlgoliaPopularClient(AlgoliaPopularClient client) {
        return client;
    }

    @Provides @Singleton
    public UserManager provideUserManager(HackerNewsClient client) {
        return client;
    }

    @Provides @Singleton
    public FeedbackClient provideFeedbackClient(FeedbackClient.Impl client) {
        return client;
    }

    @Provides @Singleton
    public ReadabilityClient provideReadabilityClient(ReadabilityClient.Impl client) {
        return client;
    }

    @Provides @Singleton
    public FavoriteManager provideFavoriteManager() {
        return new FavoriteManager();
    }

    @Provides @Singleton
    public SessionManager provideSessionManager() {
        return new SessionManager();
    }

    @Provides @Singleton
    public RestServiceFactory provideRestServiceFactory(Call.Factory callFactory) {
        return new RestServiceFactory.Impl(callFactory);
    }

    @Provides @Singleton
    public ActionViewResolver provideActionViewResolver() {
        return new ActionViewResolver();
    }

    @Provides
    public AlertDialogBuilder provideAlertDialogBuilder() {
        return new AlertDialogBuilder.Impl();
    }

    @Provides @Singleton
    public UserServices provideUserServices(Call.Factory callFactory) {
        return new UserServicesClient(callFactory);
    }

    @Provides @Singleton
    public Call.Factory provideCallFactory(Context context) {
        return new OkHttpClient.Builder()
                .cache(new Cache(context.getApplicationContext().getCacheDir(), CACHE_SIZE))
                .addNetworkInterceptor(new CacheOverrideNetworkInterceptor())
                .addInterceptor(new ConnectionAwareInterceptor(context))
                .addInterceptor(new LoggingInterceptor())
                .followRedirects(false)
                .cookieJar(new CookieJar())
                .build();
    }

    @Provides
    public AccountManager provideAccountManager(Context context) {
        return AccountManager.get(context);
    }

    @Provides
    public PopupMenu providePopupMenu() {
        return new PopupMenu.Impl();
    }

    @Provides @Singleton
    public CustomTabsDelegate provideCustomTabsDelegate() {
        return new CustomTabsDelegate();
    }

    @Provides @Singleton
    public VolumeNavigationDelegate provideVolumeNavigationDelegate() {
        return new VolumeNavigationDelegate();
    }

    static class ConnectionAwareInterceptor implements Interceptor {

        static final Map<String, String> CACHE_ENABLED_HOSTS = new HashMap<>();
        static {
            CACHE_ENABLED_HOSTS.put(HackerNewsClient.HOST,
                    RestServiceFactory.CACHE_CONTROL_MAX_AGE_30M);
            CACHE_ENABLED_HOSTS.put(AlgoliaClient.HOST,
                    RestServiceFactory.CACHE_CONTROL_MAX_AGE_30M);
            CACHE_ENABLED_HOSTS.put(ReadabilityClient.HOST,
                    RestServiceFactory.CACHE_CONTROL_MAX_AGE_24H);
        }
        private final Context mContext;

        public ConnectionAwareInterceptor(Context context) {
            mContext = context.getApplicationContext();
        }

        @Override
        public Response intercept(Chain chain) throws IOException {
            Request request = chain.request();
            boolean forceCache = CACHE_ENABLED_HOSTS.containsKey(request.url().host()) &&
                    !AppUtils.hasConnection(mContext);
            return chain.proceed(forceCache ?
                    request.newBuilder()
                            .cacheControl(CacheControl.FORCE_CACHE)
                            .build() :
                    request);
        }
    }

    public static class CacheOverrideNetworkInterceptor implements Interceptor {

        @Override
        public Response intercept(Chain chain) throws IOException {
            Request request = chain.request();
            Response response = chain.proceed(request);
            if (!ConnectionAwareInterceptor.CACHE_ENABLED_HOSTS
                    .containsKey(request.url().host())) {
                return response;
            } else {
                return response.newBuilder()
                        .header("Cache-Control",
                                ConnectionAwareInterceptor.CACHE_ENABLED_HOSTS
                                        .get(request.url().host()))
                        .build();
            }
        }
    }

    static class LoggingInterceptor implements Interceptor {
        private final Interceptor debugInterceptor = new HttpLoggingInterceptor(
                message -> Log.d(TAG_OK_HTTP, message))
                .setLevel(BuildConfig.DEBUG ?
                        HttpLoggingInterceptor.Level.BODY :
                        HttpLoggingInterceptor.Level.NONE);

        @Override
        public Response intercept(Chain chain) throws IOException {
            return debugInterceptor.intercept(chain);
        }
    }

    static class CookieJar implements okhttp3.CookieJar {

        private final HashMap<HttpUrl, List<Cookie>> cookieStore = new HashMap<>();

        @Override
        public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
            if (cookies == null) {
                return;
            }
            // accept original server
            ArrayList<Cookie> originalCookies = new ArrayList<>();
            //noinspection Convert2streamapi
            for (Cookie cookie : cookies) {
                if (HttpCookie.domainMatches(cookie.domain(), url.host())) {
                    originalCookies.add(cookie);
                }
            }
            cookieStore.put(url, originalCookies);
        }

        @Override
        public List<Cookie> loadForRequest(HttpUrl url) {
            List<Cookie> cookies = cookieStore.get(url);
            return cookies != null ? cookies : new ArrayList<>();
        }
    }
}
