package com.hackathon.covid.data

data class UserRequest (
    var name: String ?= "",
    var phone: String ?= "",
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    var packets: Int = 0
)