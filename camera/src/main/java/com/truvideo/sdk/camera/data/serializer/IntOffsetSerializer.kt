package com.truvideo.sdk.camera.data.serializer

import androidx.compose.ui.unit.IntOffset
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object IntOffsetSerializer : KSerializer<IntOffset> {
    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("IntOffset") {
            element("x", Int.serializer().descriptor)
            element("y", Int.serializer().descriptor)
        }

    override fun serialize(encoder: Encoder, value: IntOffset) {
        val composite = encoder.beginStructure(descriptor)
        composite.encodeIntElement(descriptor, 0, value.x)
        composite.encodeIntElement(descriptor, 1, value.y)
        composite.endStructure(descriptor)
    }

    override fun deserialize(decoder: Decoder): IntOffset {
        val dec = decoder.beginStructure(descriptor)
        var x = 0
        var y = 0
        loop@ while (true) {
            when (val index = dec.decodeElementIndex(descriptor)) {
                0 -> x = dec.decodeIntElement(descriptor, 0)
                1 -> y = dec.decodeIntElement(descriptor, 1)
                CompositeDecoder.DECODE_DONE -> break@loop
                else -> throw SerializationException("Unexpected index: $index")
            }
        }
        dec.endStructure(descriptor)
        return IntOffset(x, y)
    }
}