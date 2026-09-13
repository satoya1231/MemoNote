package com.example.memonote

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class NoteStore(context: Context) {
    private val preferences = context.getSharedPreferences("memo_note", Context.MODE_PRIVATE)

    fun load(): List<Note> {
        val raw = preferences.getString(KEY_NOTES, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    add(
                        Note(
                            id = item.getLong("id"),
                            title = item.optString("title"),
                            content = item.optString("content"),
                            updatedAt = item.getLong("updatedAt")
                        )
                    )
                }
            }.sortedByDescending(Note::updatedAt)
        }.getOrElse { emptyList() }
    }

    fun save(notes: List<Note>) {
        val array = JSONArray()
        notes.forEach { note ->
            array.put(
                JSONObject().apply {
                    put("id", note.id)
                    put("title", note.title)
                    put("content", note.content)
                    put("updatedAt", note.updatedAt)
                }
            )
        }
        preferences.edit().putString(KEY_NOTES, array.toString()).apply()
    }

    private companion object {
        const val KEY_NOTES = "notes"
    }
}
