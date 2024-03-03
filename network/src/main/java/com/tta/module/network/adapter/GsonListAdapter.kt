package com.tta.module.network.adapter

import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.lang.reflect.Type
import java.util.*

class GsonListAdapter : JsonDeserializer<List<*>> {
    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): List<*> {
        return if (json?.isJsonArray == true) {
            GsonBuilder().create().fromJson(json, typeOfT)
        } else {
            Collections.EMPTY_LIST
        }
    }
}