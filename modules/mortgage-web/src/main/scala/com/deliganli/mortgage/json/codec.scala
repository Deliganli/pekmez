package com.deliganli.mortgage
package json

import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.github.plokhotnyuk.jsoniter_scala.macros.*

import model.*

given JsonValueCodec[Inquiry]         = JsonCodecMaker.make(CodecMakerConfig.withFieldNameMapper(JsonCodecMaker.`enforce-kebab-case`))
given JsonValueCodec[Option[Inquiry]] = JsonCodecMaker.make(CodecMakerConfig.withFieldNameMapper(JsonCodecMaker.`enforce-kebab-case`))
