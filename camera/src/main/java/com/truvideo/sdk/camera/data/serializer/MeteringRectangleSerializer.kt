package com.truvideo.sdk.camera.data.serializer

import android.graphics.Rect
import android.hardware.camera2.params.MeteringRectangle
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object MeteringRectangleSerializer : KSerializer<MeteringRectangle> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("MeteringRectangle") {
        element("left", Int.serializer().descriptor)
        element("top", Int.serializer().descriptor)
        element("right", Int.serializer().descriptor)
        element("bottom", Int.serializer().descriptor)
        element("weight", Int.serializer().descriptor)
    }

    override fun serialize(encoder: Encoder, value: MeteringRectangle) {
        val composite = encoder.beginStructure(descriptor)
        val rect = value.rect
        composite.encodeIntElement(descriptor, 0, rect.left)
        composite.encodeIntElement(descriptor, 1, rect.top)
        composite.encodeIntElement(descriptor, 2, rect.right)
        composite.encodeIntElement(descriptor, 3, rect.bottom)
        composite.encodeIntElement(descriptor, 4, value.meteringWeight)
        composite.endStructure(descriptor)
    }

    override fun deserialize(decoder: Decoder): MeteringRectangle {
        val dec = decoder.beginStructure(descriptor)
        var left = 0
        var top = 0
        var right = 0
        var bottom = 0
        var weight = 0

        loop@ while (true) {
            when (val index = dec.decodeElementIndex(descriptor)) {
                0 -> left = dec.decodeIntElement(descriptor, 0)
                1 -> top = dec.decodeIntElement(descriptor, 1)
                2 -> right = dec.decodeIntElement(descriptor, 2)
                3 -> bottom = dec.decodeIntElement(descriptor, 3)
                4 -> weight = dec.decodeIntElement(descriptor, 4)
                CompositeDecoder.DECODE_DONE -> break@loop
                else -> throw SerializationException("Unexpected index: $index")
            }
        }

        dec.endStructure(descriptor)
        return MeteringRectangle(Rect(left, top, right, bottom), weight)
    }
}