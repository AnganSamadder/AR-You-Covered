package com.aryoucovered.app.feature.game

data class Attributes(
    var modelPath: String = "models/mii.glb",
    var profilePicture: String = "none",
    var description: String = "test",
    var type: String = "nothing",
    var identifier: String = "",
    var scale: Float = 0.5f,
    var movement_speed: Float = 0.0f
)