package com.droidnest.tech.pysadmin.domain.models

data class ProfileData(
    val country: String = "",
    val phone: String = "",
    val address: String = "",
    val dateOfBirth: String = "",
    val profilePictureUrl: String = "",
    val referredBy: String = ""
)