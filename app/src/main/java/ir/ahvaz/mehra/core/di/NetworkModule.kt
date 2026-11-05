package ir.ahvaz.mehra.core.di

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ir.ahvaz.mehra.BuildConfig
import ir.ahvaz.mehra.core.common.Constants
import ir.ahvaz.mehra.core.network.AuthInterceptor
import ir.ahvaz.mehra.core.network.ServerUrlInterceptor
import ir.ahvaz.mehra.core.network.TokenAuthenticator
import ir.ahvaz.mehra.data.remote.api.AuthApi
import ir.ahvaz.mehra.data.remote.api.DashboardApi
import ir.ahvaz.mehra.data.remote.api.FinanceApi
import ir.ahvaz.mehra.data.remote.api.GuestsApi
import ir.ahvaz.mehra.data.remote.api.HousekeepingApi
import ir.ahvaz.mehra.data.remote.api.MaintenanceApi
import ir.ahvaz.mehra.data.remote.api.ReceptionApi
import ir.ahvaz.mehra.data.remote.api.ReportsApi
import ir.ahvaz.mehra.data.remote.api.ReservationsApi
import ir.ahvaz.mehra.data.remote.api.RoomsApi
import ir.ahvaz.mehra.data.remote.api.SystemApi
import ir.ahvaz.mehra.data.remote.api.UsersApi
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideGson(): Gson = GsonBuilder()
        .setLenient()
        .serializeNulls()
        .create()

    /**
     * Base client: runtime host rewriting + bearer/device headers, no authenticator.
     * Used directly for token refresh and for the pre-login "test connection" call.
     */
    @Provides
    @Singleton
    @RefreshClient
    fun providePlainClient(
        serverUrlInterceptor: ServerUrlInterceptor,
        authInterceptor: AuthInterceptor,
    ): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(Constants.CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(Constants.READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(Constants.WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .addInterceptor(serverUrlInterceptor)
            .addInterceptor(authInterceptor)

        if (BuildConfig.DEBUG) {
            // BASIC only: bodies carry guest PII and must never reach logcat.
            val logging = HttpLoggingInterceptor { message ->
                val sanitised = message
                    .replace(Regex("Bearer [A-Za-z0-9._\\-]+"), "Bearer ***")
                    .replace(Regex("access_token=[^&\\s]+"), "access_token=***")
                android.util.Log.d("MEHRA-HTTP", sanitised)
            }.apply { level = HttpLoggingInterceptor.Level.BASIC }
            builder.addInterceptor(logging)
        }
        return builder.build()
    }

    /** Client used by Retrofit: adds automatic 401 recovery. */
    @Provides
    @Singleton
    @ApiClient
    fun provideApiClient(
        @RefreshClient plain: OkHttpClient,
        tokenAuthenticator: TokenAuthenticator,
    ): OkHttpClient = plain.newBuilder()
        .authenticator(tokenAuthenticator)
        .build()

    /** Long-lived WebSocket client for SignalR (ping keeps the tunnel alive). */
    @Provides
    @Singleton
    @RealtimeClient
    fun provideRealtimeClient(@RefreshClient plain: OkHttpClient): OkHttpClient =
        plain.newBuilder()
            .pingInterval(20, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .retryOnConnectionFailure(true)
            .build()

    @Provides
    @Singleton
    fun provideRetrofit(@ApiClient client: OkHttpClient, gson: Gson): Retrofit = Retrofit.Builder()
        // The real host is injected at request time by ServerUrlInterceptor.
        .baseUrl("https://placeholder.invalid/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi = retrofit.create(AuthApi::class.java)

    @Provides
    @Singleton
    fun provideSystemApi(retrofit: Retrofit): SystemApi = retrofit.create(SystemApi::class.java)

    @Provides
    @Singleton
    fun provideDashboardApi(retrofit: Retrofit): DashboardApi = retrofit.create(DashboardApi::class.java)

    @Provides
    @Singleton
    fun provideRoomsApi(retrofit: Retrofit): RoomsApi = retrofit.create(RoomsApi::class.java)

    @Provides
    @Singleton
    fun provideGuestsApi(retrofit: Retrofit): GuestsApi = retrofit.create(GuestsApi::class.java)

    @Provides
    @Singleton
    fun provideReservationsApi(retrofit: Retrofit): ReservationsApi =
        retrofit.create(ReservationsApi::class.java)

    @Provides
    @Singleton
    fun provideReceptionApi(retrofit: Retrofit): ReceptionApi = retrofit.create(ReceptionApi::class.java)

    @Provides
    @Singleton
    fun provideFinanceApi(retrofit: Retrofit): FinanceApi = retrofit.create(FinanceApi::class.java)

    @Provides
    @Singleton
    fun provideHousekeepingApi(retrofit: Retrofit): HousekeepingApi =
        retrofit.create(HousekeepingApi::class.java)

    @Provides
    @Singleton
    fun provideMaintenanceApi(retrofit: Retrofit): MaintenanceApi =
        retrofit.create(MaintenanceApi::class.java)

    @Provides
    @Singleton
    fun provideUsersApi(retrofit: Retrofit): UsersApi = retrofit.create(UsersApi::class.java)

    @Provides
    @Singleton
    fun provideReportsApi(retrofit: Retrofit): ReportsApi = retrofit.create(ReportsApi::class.java)
}
