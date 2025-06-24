package com.truvideo.sdk.camera.data.serializer

import android.graphics.Rect
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object RectSerializer : KSerializer<Rect> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("Rect") {
        element("left", Int.serializer().descriptor)
        element("top", Int.serializer().descriptor)
        element("right", Int.serializer().descriptor)
        element("bottom", Int.serializer().descriptor)
    }

    override fun serialize(encoder: Encoder, value: Rect) {
        val composite = encoder.beginStructure(descriptor)
        composite.encodeIntElement(descriptor, 0, value.left)
        composite.encodeIntElement(descriptor, 1, value.top)
        composite.encodeIntElement(descriptor, 2, value.right)
        composite.encodeIntElement(descriptor, 3, value.bottom)
        composite.endStructure(descriptor)
    }

    override fun deserialize(decoder: Decoder): Rect {
        val dec = decoder.beginStructure(descriptor)
        var left = 0
        var top = 0
        var right = 0
        var bottom = 0

        loop@ while (true) {
            when (val index = dec.decodeElementIndex(descriptor)) {
                0 -> left = dec.decodeIntElement(descriptor, 0)
                1 -> top = dec.decodeIntElement(descriptor, 1)
                2 -> right = dec.decodeIntElement(descriptor, 2)
                3 -> bottom = dec.decodeIntElement(descriptor, 3)
                CompositeDecoder.DECODE_DONE -> break@loop
                else -> throw SerializationException("Unexpected index: $index")
            }
        }

        dec.endStructure(descriptor)
        return Rect(left, top, right, bottom)
    }
}