package com.essential.essspace.room

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromStringList(value: List<String>?): String? {
        return value?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun toStringList(value: String?): List<String> {
        return value?.let {
            val listType = object : TypeToken<List<String>>() {}.type
            gson.fromJson(it, listType)
        } ?: emptyList()
    }

    @TypeConverter
    fun fromIntList(value: List<Int>?): String? {
        return value?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun toIntList(value: String?): List<Int> {
        return value?.let {
            val listType = object : TypeToken<List<Int>>() {}.type
            gson.fromJson(it, listType)
        } ?: emptyList()
    }
}