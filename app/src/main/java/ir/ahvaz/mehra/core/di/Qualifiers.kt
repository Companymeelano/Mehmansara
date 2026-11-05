package ir.ahvaz.mehra.core.di

import javax.inject.Qualifier

/** OkHttp client that never attaches the AuthInterceptor/Authenticator (used to refresh). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class RefreshClient

/** Fully configured client used by Retrofit. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApiClient

/** Bare client for SignalR (websocket upgrade + long lived connection). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class RealtimeClient

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher
