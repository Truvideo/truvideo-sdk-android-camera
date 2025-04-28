package com.truvideo.sdk.camera.ui.activities.arcamera

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.TextView
import com.google.ar.core.Anchor
import com.google.ar.core.DepthPoint
import com.google.ar.core.Plane
import com.google.ar.core.Point
import com.google.ar.core.TrackingState
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.FrameTime
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.Scene
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.Color
import com.google.ar.sceneform.rendering.Material
import com.google.ar.sceneform.rendering.MaterialFactory
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.Renderable
import com.google.ar.sceneform.rendering.ShapeFactory
import com.google.ar.sceneform.rendering.ViewRenderable
import com.google.ar.sceneform.ux.ArFragment
import com.truvideo.sdk.camera.R
import com.truvideo.sdk.camera.ui.activities.arcamera.model.ArrowNodeHistory
import com.truvideo.sdk.camera.ui.activities.arcamera.model.NodeHistory
import com.truvideo.sdk.camera.ui.activities.arcamera.model.RulerEndPointNodeHistory
import com.truvideo.sdk.camera.ui.activities.arcamera.model.RulerLineNode
import com.truvideo.sdk.camera.ui.activities.arcamera.model.RulerStartPointNodeHistory
import java.util.Stack

internal class ARCamera(
    private val context: Context,
    private val arFragment: ArFragment
) : Scene.OnUpdateListener {

    private var firstTime = true

    private var nodeHistory: Stack<NodeHistory> = Stack()
    private var arMode = ARModeState.RULER
    private var arMeasure = ARMeasureState.CM

    // Pointer
    private var pointerNode: AnchorNode? = null

    // Ruler
    private var rulerLiveNode: AnchorNode? = null
    private var rulerStartNode: AnchorNode? = null
    private var rulerLinesData = mutableListOf<RulerLineNode>()
    private var rulerLinesNodes = mutableMapOf<Long, Node>()
    private var rulerTextNodes = mutableMapOf<Long, Node>()
    private var rulerLiveTextNode: Node? = null

    private fun createPointer(callback: (renderable: Renderable) -> Unit) {
        MaterialFactory.makeOpaqueWithColor(
            context,
            Color(android.graphics.Color.WHITE)
        ).thenAccept {
            ModelRenderable.builder()
                .setSource(context, Uri.parse("crosshair.glb"))
                .setIsFilamentGltf(true)
                .build()
                .thenAccept {
                    it.isShadowCaster = false
                    it.isShadowReceiver = false
                    callback(it)
                }
        }
    }

    private fun createRulerTextRenderable(callback: (renderable: ViewRenderable) -> Unit) {
        ViewRenderable.builder().setView(context, R.layout.text_distance).build()
            .thenAccept {
                it.isShadowCaster = false
                it.isShadowReceiver = false
                callback(it)
            }
    }

    private fun createRulerPoint(callback: (renderable: Renderable) -> Unit) {
        MaterialFactory.makeTransparentWithColor(context, Color(android.graphics.Color.WHITE))
            .thenAccept { material ->
                val sphere = ShapeFactory.makeSphere(0.005F, Vector3.zero(), material)
                sphere.isShadowCaster = false
                sphere.isShadowReceiver = false
                callback(sphere)
            }
    }

    private fun createRulerLine(callback: (renderable: Renderable) -> Unit) {
        MaterialFactory.makeOpaqueWithColor(
            context,
            Color(android.graphics.Color.WHITE)
        ).thenAccept { material: Material? ->
            val line = ShapeFactory.makeCylinder(
                0.001f,
                1.0f,
                Vector3.zero(),
                material
            )
            line.isShadowCaster = false
            line.isShadowReceiver = false
            callback(line)
        }
    }

    private fun createArrow(callback: (renderable: Renderable) -> Unit) {
        MaterialFactory.makeOpaqueWithColor(
            context,
            Color(android.graphics.Color.WHITE)
        ).thenAccept {
            ModelRenderable.builder()
                .setSource(context, Uri.parse("arrow.glb"))
                .setIsFilamentGltf(true)
                .build()
                .thenAccept {
                    it.isShadowCaster = false
                    it.isShadowReceiver = false
                    callback(it)
                }
        }
    }

    private fun formatDistance(distance: Float): String {
        return when (arMeasure) {
            ARMeasureState.CM -> String.format("%.2f cm", distance * 100)
            ARMeasureState.IN -> String.format("%.2f in", distance * 39.3701f)
        }
    }

    fun updateMode(value: ARModeState) {
        if (arMode == value) return

        arMode = value

        when (value) {
            ARModeState.OBJECT -> {

            }

            ARModeState.RULER -> {
                if (rulerStartNode != null) {
                    undo()
                }
            }

            ARModeState.RECORD -> {
            }
        }
    }

    fun updateMeasure(value: ARMeasureState) {
        arMeasure = value
    }

    fun undo() {
        if (nodeHistory.isEmpty()) return

        val last = nodeHistory.pop()
        Log.d("TruvideoSdkCamera", "Pop $last")

        when (last) {
            is ArrowNodeHistory -> {
                val node = last.node
                arFragment.arSceneView.scene.removeChild(node)
            }

            is RulerStartPointNodeHistory -> {
                rulerStartNode = null
                val node = last.point
                arFragment.arSceneView.scene.removeChild(node)
            }

            is RulerEndPointNodeHistory -> {
                arFragment.arSceneView.scene.removeChild(last.point)
                arFragment.arSceneView.scene.removeChild(last.text)
                arFragment.arSceneView.scene.removeChild(last.line)

                // call again this to pop the start point
                undo()
            }
        }
    }

    fun undoAll() {
        while (nodeHistory.isNotEmpty()) {
            undo()
        }
    }

    private fun screenCenter(): Vector3 {
        val vw = arFragment.arSceneView
        return Vector3(vw.width / 2f, vw.height / 2f, 0f)
    }

    private fun addObject() {
        val anchor = calculatePointerAnchor() ?: return

        val node = AnchorNode(anchor).apply {
            setParent(arFragment.arSceneView.scene)
            createArrow { setRenderable((it)) }
        }
        node.setParent(arFragment.arSceneView.scene)
        arFragment.arSceneView.scene.addChild(node)

        nodeHistory.add(
            ArrowNodeHistory(
                id = System.currentTimeMillis(),
                node = node
            )
        )
    }

    private fun calculatePointerAnchor(): Anchor? {
        val frame = arFragment.arSceneView.arFrame ?: return null
        val hit = frame.hitTest(screenCenter().x, screenCenter().y).firstOrNull()
        if (hit == null) {
            Log.d("TruvideoSdkCamera", "No hit test")
            return null
        }

        try {
            return when (val trackable = hit.trackable) {
                is Plane -> {
                    if (trackable.isPoseInExtents(hit.hitPose) || trackable.isPoseInPolygon(hit.hitPose)) {
                        trackable.createAnchor(hit.hitPose)
                    } else {
                        Log.d("TruvideoSdkCamera", "Hit on plane its not on extents or polygon")
                        null
                    }
                }

                is Point -> {
                    trackable.createAnchor(hit.hitPose)
                }

                is DepthPoint -> {
                    if (trackable.trackingState == TrackingState.TRACKING) {
                        trackable.createAnchor(hit.hitPose)
                    } else {
                        Log.d("TruvideoSdkCamera", "Hit on depth point is not tracking")
                        null
                    }
                }

                else -> {
                    Log.d("TruvideoSdkCamera", "Hit on unknown trackable. ${trackable}")
                    null
                }
            }
        } catch (exception: Exception) {
            Log.d("TruvideoSdkCamera", "Error creating pointer node", exception)
            return null
        }
    }

    private fun drawPointerNode() {
        try {
            val anchor = calculatePointerAnchor()
            if (anchor == null) {
                clearPointerNode()
                return
            }

            if (pointerNode == null) {
                val anchorNode = AnchorNode(anchor).apply {
                    setParent(arFragment.arSceneView.scene)
                    createPointer { setRenderable(it) }
                }

                pointerNode = anchorNode
                arFragment.arSceneView.scene.addChild(anchorNode)
            } else {
                pointerNode?.anchor = anchor
            }
        } catch (exception: Exception) {
            exception.printStackTrace()
            clearPointerNode()
        }
    }

    private fun clearPointerNode() {
        if (pointerNode != null) {
            arFragment.arSceneView.scene.removeChild(pointerNode)
            pointerNode = null
        }
    }

    private fun drawRulerLiveLine() {
        if (rulerStartNode == null || pointerNode == null) {
            clearRulerLiveLine()
            return
        }

        val a = rulerStartNode!!.worldPosition
        val b = pointerNode!!.worldPosition
        val difference = Vector3.subtract(b, a)
        val distance = difference.length()

        val node = if (rulerLiveNode == null) {
            val node = AnchorNode().apply {
                setParent(arFragment.arSceneView.scene)
                createRulerLine { setRenderable(it) }
            }
            arFragment.arSceneView.scene.addChild(node)
            rulerLiveNode = node
            node
        } else {
            rulerLiveNode!!
        }

        node.worldScale = Vector3(1.0f, distance, 1.0f)
        node.worldPosition = Vector3.add(a, b).scaled(0.5f)
        node.worldRotation = Quaternion.lookRotation(difference.normalized(), Vector3.up())
        node.localRotation = Quaternion.multiply(
            node.localRotation,
            Quaternion.axisAngle(Vector3(1.0f, 0.0f, 0.0f), 90f)
        )
    }

    private fun clearRulerLiveLine() {
        if (rulerLiveNode != null) {
            arFragment.arSceneView.scene.removeChild(rulerLiveNode)
            rulerLiveNode = null
        }
    }

    private fun addRulerPoint() {
        val pointer = pointerNode
        if (pointer == null) {
            Log.d("TruvideoSdkCamera", "Cannot add ruler point, pointer node is null")
            return
        }

        val node = AnchorNode(pointer.anchor).apply {
            parent = arFragment.arSceneView.scene
            createRulerPoint { setRenderable(it) }
        }
        arFragment.arSceneView.scene.addChild(node)

        if (rulerStartNode == null) {
            rulerStartNode = node
            nodeHistory.add(
                RulerStartPointNodeHistory(
                    id = System.currentTimeMillis(),
                    point = node
                )
            )
        } else {
            val lineId = System.currentTimeMillis()
            rulerLinesData.add(
                RulerLineNode(
                    id = lineId,
                    nodeA = rulerStartNode!!,
                    nodeB = node
                )
            )
            drawRules()

            val lineNode = rulerLinesNodes[lineId]
            val textNode = rulerTextNodes[lineId]
            if (lineNode != null && textNode != null) {
                nodeHistory.add(
                    RulerEndPointNodeHistory(
                        id = System.currentTimeMillis(),
                        point = node,
                        line = lineNode,
                        text = textNode
                    )
                )
            }

            rulerStartNode = null
        }
    }

    private fun clearRulerPoint() {
        if (rulerStartNode != null) {
            arFragment.arSceneView.scene.removeChild(rulerStartNode)
            rulerStartNode = null
        }
    }

    private fun drawRules() {
        rulerLinesData.forEach { line ->
            val lineNode = if (rulerLinesNodes[line.id] == null) {
                val node = Node().apply {
                    setParent(arFragment.arSceneView.scene)
                    createRulerLine { setRenderable(it) }
                }
                arFragment.arSceneView.scene.addChild(node)
                rulerLinesNodes[line.id] = node
                node
            } else {
                rulerLinesNodes[line.id]!!
            }

            val textNode = if (rulerTextNodes[line.id] == null) {
                val node = Node().apply {
                    setParent(arFragment.arSceneView.scene)
                    createRulerTextRenderable { renderable ->
                        setRenderable(renderable)
                    }
                }
                arFragment.arSceneView.scene.addChild(node)
                rulerTextNodes[line.id] = node
                node
            } else {
                rulerTextNodes[line.id]!!
            }


            val a = line.nodeA.worldPosition
            val b = line.nodeB.worldPosition
            val difference = Vector3.subtract(b, a)
            val distance = difference.length()

            // Line
            lineNode.worldScale = Vector3(1.0f, distance, 1.0f)
            lineNode.worldPosition = Vector3.add(a, b).scaled(0.5f)
            lineNode.worldRotation = Quaternion.lookRotation(difference.normalized(), Vector3.up())
            lineNode.localRotation = Quaternion.multiply(
                lineNode.localRotation,
                Quaternion.axisAngle(Vector3(1.0f, 0.0f, 0.0f), 90f)
            )

            // Text
            val textView = (textNode.renderable as ViewRenderable?)?.view?.findViewById<TextView>(R.id.distanceTextView)
            textView?.text = formatDistance(distance)

            textNode.worldScale = Vector3(0.2f, 0.2f, 0.2f)
            textNode.worldPosition = Vector3.add(a, b).scaled(0.5f)

            val cameraRotation = arFragment.arSceneView.scene.camera.worldRotation
            textNode.worldRotation = cameraRotation

            textNode.localRotation = Quaternion.multiply(
                textNode.localRotation,
                Quaternion.axisAngle(Vector3(0.0f, 0.0f, 1.0f), 0f)
            )
        }
    }

    private fun drawRulerLiveText() {
        if (rulerStartNode == null || pointerNode == null) {
            clearRulerLiveText()
            return
        }

        val node = if (rulerLiveTextNode == null) {
            val node = Node().apply {
                setParent(arFragment.arSceneView.scene)
                createRulerTextRenderable { renderable ->
                    setRenderable(renderable)
                }
            }
            rulerLiveTextNode = node
            arFragment.arSceneView.scene.addChild(node)
            node
        } else {
            rulerLiveTextNode!!
        }

        val a = rulerStartNode!!.worldPosition
        val b = pointerNode!!.worldPosition
        val difference = Vector3.subtract(b, a)
        val distance = difference.length()

        val textView = (node.renderable as ViewRenderable?)?.view?.findViewById<TextView>(R.id.distanceTextView)
        textView?.text = formatDistance(distance)

        node.worldScale = Vector3(0.2f, 0.2f, 0.2f)
        node.worldPosition = Vector3.add(a, b).scaled(0.5f)

        val cameraRotation = arFragment.arSceneView.scene.camera.worldRotation
        node.worldRotation = cameraRotation

        node.localRotation = Quaternion.multiply(
            node.localRotation,
            Quaternion.axisAngle(Vector3(0.0f, 0.0f, 1.0f), 0f)
        )
    }

    private fun clearRulerLiveText() {
        if (rulerLiveTextNode != null) {
            arFragment.arSceneView.scene.removeChild(rulerLiveTextNode)
            rulerLiveTextNode = null
        }
    }

    private fun onPressed() {
        when (arMode) {
            ARModeState.OBJECT -> {
                addObject()
            }

            ARModeState.RULER -> {
                addRulerPoint()
            }

            ARModeState.RECORD -> {
                // Nothing to do
            }
        }
    }

    override fun onUpdate(frameTime: FrameTime?) {
        if (firstTime) {
            firstTime = false

            arFragment.arSceneView.setOnClickListener {
                onPressed()
            }
        }

        drawRules()

        when (arMode) {
            ARModeState.OBJECT -> {
                drawPointerNode()
                clearRulerPoint()
                clearRulerLiveLine()
                clearRulerLiveText()
            }

            ARModeState.RULER -> {
                drawPointerNode()
                drawRulerLiveLine()
                drawRulerLiveText()
            }

            ARModeState.RECORD -> {
                clearPointerNode()
                clearRulerPoint()
                clearRulerLiveLine()
                clearRulerLiveText()
            }
        }
    }
}