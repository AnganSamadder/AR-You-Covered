package com.aryoucovered.app.feature.game

import io.github.sceneview.loaders.ModelLoader
import io.github.sceneview.math.Position
import io.github.sceneview.node.ModelNode
import io.github.sceneview.node.Node

data class GameObject(
    val node: Node,
    val attr: Attributes,
    var placed: Boolean = false
)