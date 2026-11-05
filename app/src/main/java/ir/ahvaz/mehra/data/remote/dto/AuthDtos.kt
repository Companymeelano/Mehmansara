package ir.ahvaz.mehra.data.remote.dto

import com.google.gson.annotations.SerializedName

data class LoginRequestDto(
    @SerializedName("username") val username: String,
    @SerializedName("password") val password: String,
    @SerializedName("deviceId") val deviceId: String? = null,
    @SerializedName("deviceName") val deviceName: String? = null,
)

data class RefreshRequestDto(
    @SerializedName("refreshToken") val refreshToken: String,
    @SerializedName("deviceId") val deviceId: String? = null,
)

data class LogoutRequestDto(
    @SerializedName("refreshToken") val refreshToken: String? = null,
    @SerializedName("deviceId") val deviceId: String? = null,
)

data class AuthResponseDto(
    @SerializedName("accessToken") val accessToken: String? = null,
    @SerializedName("refreshToken") val refreshToken: String? = null,
    @SerializedName("accessExpiresAtUtc") val accessExpiresAtUtc: String? = null,
    @SerializedName("refreshExpiresAtUtc") val refreshExpiresAtUtc: String? = null,
    @SerializedName("user") val user: SessionUserDto? = null,
)

data class SessionUserDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("fullName") val fullName: String? = null,
    @SerializedName("username") val username: String? = null,
    @SerializedName("role") val role: String? = null,
    @SerializedName("roleTitle") val roleTitle: String? = null,
    @SerializedName("permissions") val permissions: List<String>? = null,
)

data class ChangePasswordRequestDto(
    @SerializedName("currentPassword") val currentPassword: String,
    @SerializedName("newPassword") val newPassword: String,
)

data class ServerInfoDto(
    @SerializedName("serverVersion") val serverVersion: String? = null,
    @SerializedName("apiVersion") val apiVersion: String? = null,
    @SerializedName("serverTimeUtc") val serverTimeUtc: String? = null,
    @SerializedName("databaseConnected") val databaseConnected: Boolean? = null,
)
