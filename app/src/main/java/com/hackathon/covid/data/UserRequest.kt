package com.hackathon.covid.data

class UserRequest (
    var name: String ?= "",
    var phone: String ?= "",
    var latitude: String = "",
    var longitude: String = "",
    var packets: Int = 0
)