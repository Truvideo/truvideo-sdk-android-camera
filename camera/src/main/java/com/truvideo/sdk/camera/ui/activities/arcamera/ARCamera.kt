package com.truvideo.sdk.camera.ui.activities.arcamera

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.TextView
import com.google.ar.core.Plane
import com.google.ar.core.Pose
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
import com.google.ar.sceneform.ux.TransformableNode
import com.truvideo.sdk.camera.R
import java.util.Locale
import java.util.Stack

internal class ARCamera(
    private val context: Context,
    private val arFragment: ArFragment
) : Scene.OnUpdateListener {

    private lateinit var transformableNode: TransformableNode
    private var renderable: Renderable? = null
    private var sphere: Renderable? = null
    private var isHit = false
    private var anchorNode1: Node? = null //cross-hair node
    private var anchorNode2: Node? = null //cross-hair node
    private var anchorNodeA: Node? = null //Ruler start node
    private var anchorNodeB: Node? = null //Ruler end node
    private var lineNode: Node = Node() // Create a node to hold the ruler spheres
    private var nodeHistory: Stack<NodeItem> = Stack()
    private var isTapped = false
    private var distanceTextNode = Node()
    private lateinit var distanceTextView: TextView
    private var arMode = ARModeState.RULER
    private var arMeasure = ARMeasureState.CM

    init {
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
                    renderable = it
                    renderable?.isShadowCaster = false
                }
        }

        //Setup sphere material for AR Ruler
        val grayColor = Color(0.1f, 0.1f, 0.1f, 0.001f)
        MaterialFactory.makeTransparentWithColor(context, grayColor)
            .thenAccept { material ->
                val renderable = ShapeFactory.makeSphere(0.005F, Vector3(0F, 0.006F, 0F), material)
                renderable.isShadowCaster = false
                sphere = renderable
            }

        //Setup viewRenderable for text view
        ViewRenderable.builder().setView(context, R.layout.text_distance).build()
            .thenAccept {
                it.isShadowCaster = false
                distanceTextNode.setRenderable(it)
                distanceTextNode.localScale = Vector3(0.2f, 0.2f, 0.2f)
                distanceTextView = it.view.findViewById(R.id.distanceTextView)
            }

    }


    fun updateMode(value: ARModeState) {
        arMode = value
    }

    fun updateMeasure(value: ARMeasureState) {
        arMeasure = value
    }

    fun undo() {
        undoAction(nodeHistory)
    }

    fun undoAll() {
        undoAll(nodeHistory)
    }

    private fun addObject(parentAnchorNode: Node) {
        val anchorNode = Node()
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
                    anchorNode.setRenderable(it)
                    anchorNode.worldPosition = parentAnchorNode.worldPosition
                    anchorNode.parent = arFragment.arSceneView.scene
                    val nodeItem = NodeItem(anchorNode, NodeType.ARROW)
                    nodeHistory.push(nodeItem)
                    arFragment.arSceneView.scene.addChild(anchorNode)
                }
        }
    }

    private fun addPoint(parentAnchorNode: Node, pointType: NodeType) {
        val anchorNode = Node()
        MaterialFactory.makeOpaqueWithColor(context, Color(android.graphics.Color.WHITE)).thenAccept { material ->
            val sphere: Renderable = ShapeFactory.makeSphere(
                0.005F,
                Vector3(0F, 0.003F, 0F),
                material
            )
            sphere.isShadowCaster = false
            anchorNode.worldPosition = parentAnchorNode.worldPosition
            anchorNode.parent = arFragment.arSceneView.scene
            anchorNode.setRenderable(sphere)
            val nodeItem = NodeItem(anchorNode, pointType)
            nodeHistory.push(nodeItem)
            arFragment.arSceneView.scene.addChild(anchorNode)
        }
    }

    private fun addLine() {
        val point1 = anchorNodeA?.worldPosition
        val point2 = anchorNodeB?.worldPosition

        val difference = Vector3.subtract(point2, point1)
        val distance = difference.length()

        //View Renderable to show the Distance on the surface
        ViewRenderable.builder().setView(context, R.layout.text_distance_metered).build()
            .thenAccept {
                val distanceTextNode = Node()
                it.isShadowCaster = false
                distanceTextNode.setRenderable(it)
                distanceTextNode.localScale = Vector3(0.2f, 0.2f, 0.2f) //Scale
                val textPosition = Vector3.add(point2, point1).scaled(0.5f)
                textPosition.y += 0.02f
                distanceTextNode.worldPosition = textPosition //Position
                updateTextNodeRotation(textNode = distanceTextNode) //Rotation
                val distanceTextView: TextView = it.view.findViewById(R.id.distanceTextView)
                val distanceFormatted: String = when (arMeasure) {
                    ARMeasureState.CM -> String.format(Locale.getDefault(), "%.2f cm", distance * 100)
                    ARMeasureState.IN -> String.format(Locale.getDefault(), "%.2f in", distance * 39.3701)
                }
                distanceTextView.text = distanceFormatted
                distanceTextNode.parent = arFragment.arSceneView.scene
                val nodeItem = NodeItem(distanceTextNode, NodeType.TEXT)
                nodeHistory.push(nodeItem)
            }

        val rotationFromAToB: Quaternion =
            Quaternion.lookRotation(difference.normalized(), Vector3.up())

        //Add Line Cylinder between Node A and B
        MaterialFactory.makeOpaqueWithColor(
            context,
            Color(android.graphics.Color.WHITE)
        ).thenAccept { material: Material? ->
            val renderable = ShapeFactory.makeCylinder(
                0.001f,
                distance,
                Vector3(0f, 0f, 0f),
                material
            )
            renderable.isShadowCaster = false
            val lineNode = Node()
            lineNode.parent = arFragment.arSceneView.scene
            lineNode.setRenderable(renderable)
            val worldPosition = Vector3.add(point1, point2).scaled(0.5f)
            worldPosition.y += 0.005f
            lineNode.worldPosition = worldPosition
            lineNode.worldRotation = rotationFromAToB
            val adjustment = Quaternion.axisAngle(Vector3(1.0f, 0.0f, 0.0f), 90f)
            lineNode.localRotation = Quaternion.multiply(lineNode.localRotation, adjustment)
            val nodeItem = NodeItem(lineNode, NodeType.LINE)
            nodeHistory.push(nodeItem)
        }
    }

    private fun undoAction(nodeHistory: Stack<NodeItem>) {
        if (!nodeHistory.isEmpty()) {
            when (nodeHistory.peek().nodeType) {
                NodeType.LINE, NodeType.POINT_B, NodeType.TEXT -> {
                    while (nodeHistory.peek().nodeType != NodeType.POINT_A) {
                        arFragment.arSceneView.scene.removeChild(nodeHistory.peek().node)
                        nodeHistory.pop()
                    }
                    if (nodeHistory.peek().nodeType == NodeType.POINT_A) {
                        arFragment.arSceneView.scene.removeChild(nodeHistory.peek().node)
                        nodeHistory.pop()
                    }
                }

                NodeType.POINT_A, NodeType.ARROW -> {
                    arFragment.arSceneView.scene.removeChild(nodeHistory.peek().node)
                    nodeHistory.pop()
                    removeDotsRuler()
                }
            }
            isTapped = false
            anchorNodeA = null
            anchorNodeB = null
        }
    }

    private fun undoAll(nodeHistory: Stack<NodeItem>) {
        while (!nodeHistory.isEmpty()) {
            arFragment.arSceneView.scene.removeChild(nodeHistory.peek().node)
            nodeHistory.pop()
        }
    }

    private fun screenCenter(): Vector3 {
        val vw = arFragment.arSceneView
        return Vector3(vw.width / 2f, vw.height / 2f, 0f)
    }

    /** Draw Ruler with Spheres and distance TextView */
    private fun drawDotsRuler() {
        lineNode = Node()
        lineNode.parent = arFragment.arSceneView.scene
        val point1 = anchorNode1!!.worldPosition
        val point2 = anchorNode2!!.worldPosition
        val distance = Vector3.subtract(point1, point2).length()
        val numSpheres = (distance / 0.03f).toInt() // Adjust sphere spacing
        val step = Vector3.subtract(point2, point1).normalized().scaled(0.03f)
        for (i in 0 until numSpheres) {
            val position = Vector3.add(point1, step.scaled(i.toFloat()))
            val sphereNode = Node()
            sphereNode.parent = lineNode
            sphereNode.worldPosition = position
            sphereNode.renderable = sphere
        }
        //Add distanceTextView value and position.
        val textPosition = Vector3.add(point2, point2).scaled(0.5f)
        textPosition.y += 0.02f
        distanceTextNode.worldPosition = textPosition
        val distanceFormatted: String = when (arMeasure) {
            ARMeasureState.CM -> String.format(Locale.getDefault(), "%.2f cm", distance * 100)
            ARMeasureState.IN -> String.format(Locale.getDefault(), "%.2f in", distance * 39.3701)
        }
        distanceTextView.text = distanceFormatted
        updateTextNodeRotation(distanceTextNode)
        distanceTextNode.parent = arFragment.arSceneView.scene
    }

    private fun removeDotsRuler() {
        arFragment.arSceneView.scene.removeChild(lineNode)
        arFragment.arSceneView.scene.removeChild(distanceTextNode)
    }

    /** Update Node Rotation to face Camera */
    private fun updateTextNodeRotation(textNode: Node?) {
        if (arFragment.arSceneView.scene == null || textNode == null) {
            return
        }
        val cameraPosition = arFragment.arSceneView.scene.camera.worldPosition
        val cardPosition = textNode.worldPosition
        val direction = Vector3.subtract(cameraPosition, cardPosition)
        val lookRotation = Quaternion.lookRotation(direction, Vector3.up())
        textNode.worldRotation = lookRotation
    }

    /** Updates the entire ARFragment to refresh on frameTime */
    override fun onUpdate(frameTime: FrameTime?) {

        val frame = arFragment.arSceneView.arFrame
        if (!isHit) { //set cross-hair renderable and click listener.
            isHit = true
            transformableNode = TransformableNode(arFragment.transformationSystem)
            transformableNode.renderable = renderable

            arFragment.arSceneView.setOnClickListener {
                if (anchorNode2 == null) return@setOnClickListener
                if (arMode == ARModeState.RECORD) return@setOnClickListener
                if (arMode == ARModeState.OBJECT) {
                    anchorNodeA = anchorNode2
                    anchorNodeA?.parent = arFragment.arSceneView.scene
                    anchorNode1 = anchorNode2
                    anchorNode1?.parent = arFragment.arSceneView.scene
                    addObject(anchorNodeA ?: return@setOnClickListener)
                    anchorNodeA = null
                    anchorNodeB = null
                }
                if (arMode == ARModeState.RULER) {
                    if (anchorNodeA == null) {
                        isTapped = true
                        anchorNodeA = anchorNode2
                        anchorNodeA?.parent = arFragment.arSceneView.scene
                        anchorNode1 = anchorNode2
                        anchorNode1?.parent = arFragment.arSceneView.scene
                        addPoint(anchorNodeA!!, NodeType.POINT_A)
                    } else if (anchorNodeB == null) {
                        isTapped = false
                        removeDotsRuler()
                        anchorNodeB = anchorNode2
                        anchorNodeB?.parent = arFragment.arSceneView.scene
                        addPoint(anchorNodeB!!, NodeType.POINT_B)
                        addLine()
                    } else {
                        anchorNodeA = null
                        anchorNodeB = null
                    }
                }
            }
        }

        when (arMode) { // AR Mode from ui.AROptionsPanel
            ARModeState.RECORD -> transformableNode.renderable = null
            ARModeState.RULER -> transformableNode.renderable = renderable
            ARModeState.OBJECT -> transformableNode.renderable = renderable
        }

        if (frame != null) {
            //Perform a hit test at the center of the screen to place an object without tapping
            val hitTest = frame.hitTest(screenCenter().x, screenCenter().y).firstOrNull()
            val plane = frame.getUpdatedTrackables(Plane::class.java).firstOrNull()
            if (plane != null && hitTest != null) {
                try {
                    transformableNode.isEnabled = true
                    val hitResult = hitTest.hitPose
                    val modelAnchor = plane.createAnchor(hitResult)
                    val anchorNode = AnchorNode(modelAnchor)
                    anchorNode.parent = arFragment.arSceneView.scene
                    transformableNode.parent = anchorNode
                    transformableNode.worldPosition = Vector3(
                        modelAnchor.pose.tx(),
                        modelAnchor.pose.compose(Pose.makeTranslation(0f, 0.005f, 0f)).ty(),
                        modelAnchor.pose.tz()
                    )
                    anchorNode2 = anchorNode
                    if (isTapped) {
                        removeDotsRuler()
                        anchorNode2 = anchorNode
                        drawDotsRuler()
                    }

                } catch (ex: Exception) {
                    Log.e("ARCORE", ex.toString())
                }
            }
        }

    }
}