package com.hackathon.covid.data

data class Shop (
        var shopName: String = "",
        var phone: String = "",
        var masks: Int = 0,
        var sanitizer: Int = 0,
        var latitude: Double = 0.0,
        var longitude: Double = 0.0
)