package cl.monsoon.s1next;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;

import cl.monsoon.s1next.data.User;
import cl.monsoon.s1next.data.Wifi;
import cl.monsoon.s1next.data.api.Api;
import cl.monsoon.s1next.data.api.S1Service;
import cl.monsoon.s1next.data.api.UserValidator;
import cl.monsoon.s1next.data.pref.DownloadPreferencesManager;
import cl.monsoon.s1next.data.pref.DownloadPreferencesRepository;
import cl.monsoon.s1next.data.pref.GeneralPreferencesManager;
import cl.monsoon.s1next.data.pref.GeneralPreferencesRepository;
import cl.monsoon.s1next.data.pref.ThemeManager;
import cl.monsoon.s1next.viewmodel.UserViewModel;
import cl.monsoon.s1next.widget.EventBus;
import cl.monsoon.s1next.widget.PersistentHttpCookieStore;
import dagger.Module;
import dagger.Provides;
import okhttp3.JavaNetCookieJar;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.jackson.JacksonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

/**
 * Provides instances of the objects when we need to inject.
 */
@Module
final class AppModule {

    private final App mApp;

    public AppModule(App app) {
        this.mApp = app;
    }

    @Provides
    @Singleton
    Context provideContext() {
        return mApp;
    }

    @Provides
    @Singleton
    CookieManager providerCookieManager(Context context) {
        return new CookieManager(new PersistentHttpCookieStore(context), CookiePolicy.ACCEPT_ALL);
    }

    @Provides
    @Singleton
    OkHttpClient providerOkHttpClient(CookieManager cookieManager) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.connectTimeout(17, TimeUnit.SECONDS);
        builder.writeTimeout(17, TimeUnit.SECONDS);
        builder.readTimeout(77, TimeUnit.SECONDS);
        builder.cookieJar(new JavaNetCookieJar(cookieManager));
        if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor httpLoggingInterceptor = new HttpLoggingInterceptor();
            httpLoggingInterceptor.setLevel(HttpLoggingInterceptor.Level.HEADERS);
            builder.interceptors().add(httpLoggingInterceptor);
        }

        return builder.build();
    }

    @Provides
    @Singleton
    S1Service providerRetrofit(OkHttpClient okHttpClient) {
        return new Retrofit.Builder()
                .client(okHttpClient)
                .baseUrl(Api.BASE_API_URL)
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(JacksonConverterFactory.create())
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .build()
                .create(S1Service.class);
    }

    @Provides
    @Singleton
    EventBus providerEventBus() {
        return new EventBus();
    }

    @Provides
    @Singleton
    User providerUser(UserViewModel userViewModel) {
        return userViewModel.getUser();
    }

    @Provides
    @Singleton
    UserValidator providerUserValidator(User user) {
        return new UserValidator(user);
    }

    @Provides
    @Singleton
    UserViewModel providerUserViewModel() {
        return new UserViewModel();
    }

    @Provides
    @Singleton
    Wifi providerWifi() {
        return new Wifi();
    }

    @Provides
    @Singleton
    SharedPreferences provideSharedPreferences(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    @Provides
    @Singleton
    GeneralPreferencesRepository provideGeneralPreferencesProvider(Context context, SharedPreferences sharedPreferences) {
        return new GeneralPreferencesRepository(context, sharedPreferences);
    }

    @Provides
    @Singleton
    GeneralPreferencesManager provideGeneralPreferencesManager(GeneralPreferencesRepository generalPreferencesProvider) {
        return new GeneralPreferencesManager(generalPreferencesProvider);
    }

    @Provides
    @Singleton
    ThemeManager provideThemeManager(Context context, GeneralPreferencesRepository generalPreferencesProvider) {
        return new ThemeManager(context, generalPreferencesProvider);
    }

    @Provides
    @Singleton
    DownloadPreferencesRepository provideDownloadPreferencesProvider(Context context, SharedPreferences sharedPreferences) {
        return new DownloadPreferencesRepository(context, sharedPreferences);
    }

    @Provides
    @Singleton
    DownloadPreferencesManager provideDownloadPreferencesManager(DownloadPreferencesRepository downloadPreferencesProvider, Wifi wifi) {
        return new DownloadPreferencesManager(downloadPreferencesProvider, wifi);
    }
}
