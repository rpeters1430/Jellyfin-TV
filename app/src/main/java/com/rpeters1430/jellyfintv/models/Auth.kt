package com.rpeters1430.jellyfintv.models

import com.google.gson.annotations.SerializedName

data class AuthenticateUserByNameRequest(
    @SerializedName("Username") val username: String,
    @SerializedName("Pw") val password: String
)

data class AuthenticationResult(
    @SerializedName("User") val user: UserDto?,
    @SerializedName("AccessToken") val accessToken: String?,
    @SerializedName("ServerId") val serverId: String?
)

data class UserDto(
    @SerializedName("Id") val id: String,
    @SerializedName("Name") val name: String,
    @SerializedName("ServerId") val serverId: String?
)
