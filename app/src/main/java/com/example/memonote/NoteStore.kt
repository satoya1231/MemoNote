package com.example.memonote

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class NoteStore(context: Context) {
    private val preferences = context.getSharedPreferences("memo_note", Context.MODE_PRIVATE)

    fun load(): List<Note> {
        val raw = preferences.getString(KEY_NOTES, null) ?: return emptyList()
        return parseNotes(raw)
    }

    fun exportJson(): String = preferences.getString(KEY_NOTES, "[]") ?: "[]"

    fun importJson(raw: String): List<Note> {
        val imported = parseNotesOrThrow(raw)
        save(imported)
        return imported
    }

    private fun parseNotes(raw: String): List<Note> {
        return runCatching { parseNotesOrThrow(raw) }.getOrElse { emptyList() }
    }

    private fun parseNotesOrThrow(raw: String): List<Note> {
        val array = JSONArray(raw)
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(
                    Note(
                        id = item.getLong("id"),
                        title = item.optString("title"),
                        content = item.optString("content"),
                        updatedAt = item.getLong("updatedAt"),
                        isPinned = item.optBoolean("isPinned", false),
                        deletedAt = if (item.isNull("deletedAt")) null else item.optLong("deletedAt")
                    )
                )
            }
        }.sortedForDisplay()
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
                    put("isPinned", note.isPinned)
                    put("deletedAt", note.deletedAt ?: JSONObject.NULL)
                }
            )
        }
        preferences.edit().putString(KEY_NOTES, array.toString()).apply()
    }

    private companion object {
        const val KEY_NOTES = "notes"
    }
}

fun List<Note>.sortedForDisplay(): List<Note> =
    sortedWith(compareByDescending<Note> { it.isPinned }.thenByDescending { it.updatedAt })
