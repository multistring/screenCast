package com.tta.module.network.adapter

import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.lang.reflect.Type

class GsonAnyAdapter : JsonDeserializer<Any> {
    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): Any {
        return if (json?.isJsonObject == true) {
            GsonBuilder().create().fromJson(json, typeOfT)
        } else {
            Any()
        }
    }

}