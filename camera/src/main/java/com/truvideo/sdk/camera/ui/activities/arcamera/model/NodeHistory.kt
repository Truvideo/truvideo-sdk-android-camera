package com.truvideo.sdk.camera.ui.activities.arcamera.model

import com.google.ar.sceneform.Node

open class NodeHistory(
    open val id: Long,
)

data class ArrowNodeHistory(
    override val id: Long,
    val node: Node
) : NodeHistory(id)

data class RulerStartPointNodeHistory(
    override val id: Long,
    val point: Node
) : NodeHistory(id)

data class RulerEndPointNodeHistory(
    override val id: Long,
    val point: Node,
    val line: Node,
    val text: Node,
) : NodeHistory(id)
