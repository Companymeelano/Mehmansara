package ir.ahvaz.mehra.data.remote.dto

import com.google.gson.annotations.SerializedName

data class AppUserDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("fullName") val fullName: String? = null,
    @SerializedName("username") val username: String? = null,
    @SerializedName("roleCode") val roleCode: String? = null,
    @SerializedName("roleTitle") val roleTitle: String? = null,
    @SerializedName("phone") val phone: String? = null,
    @SerializedName("isActive") val isActive: Boolean? = null,
    @SerializedName("lastLoginAtUtc") val lastLoginAtUtc: String? = null,
)

data class CreateUserRequestDto(
    @SerializedName("fullName") val fullName: String,
    @SerializedName("username") val username: String,
    @SerializedName("password") val password: String? = null,
    @SerializedName("roleCode") val roleCode: String,
    @SerializedName("phone") val phone: String? = null,
    @SerializedName("isActive") val isActive: Boolean = true,
)

data class ToggleActiveRequestDto(
    @SerializedName("isActive") val isActive: Boolean,
)

data class ResetPasswordRequestDto(
    @SerializedName("newPassword") val newPassword: String,
)

data class RoleDto(
    @SerializedName("code") val code: String? = null,
    @SerializedName("titleFa") val titleFa: String? = null,
    @SerializedName("permissions") val permissions: List<String>? = null,
    @SerializedName("userCount") val userCount: Int? = null,
)

data class AuditLogDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("action") val action: String? = null,
    @SerializedName("entity") val entity: String? = null,
    @SerializedName("entityId") val entityId: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("actorName") val actorName: String? = null,
    @SerializedName("occurredAtUtc") val occurredAtUtc: String? = null,
)
