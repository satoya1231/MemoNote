package com.example.memonote

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = NoteStore(applicationContext)
        setContent {
            MemoTheme {
                MemoApp(store)
            }
        }
    }
}

@Composable
private fun MemoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = androidx.compose.material3.lightColorScheme(
            primary = Color(0xFF4F5D95),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFDCE2FF),
            secondaryContainer = Color(0xFFE6E1F1),
            background = Color(0xFFF9F9FF),
            surface = Color(0xFFF9F9FF)
        ),
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MemoApp(store: NoteStore) {
    var notes by remember { mutableStateOf(store.load()) }
    var searchText by rememberSaveable { mutableStateOf("") }
    var editorNote by remember { mutableStateOf<Note?>(null) }
    var showEditor by rememberSaveable { mutableStateOf(false) }
    var showDeleteConfirmation by rememberSaveable { mutableStateOf(false) }

    val filteredNotes = remember(notes, searchText) {
        val query = searchText.trim()
        if (query.isEmpty()) notes
        else notes.filter { note ->
            note.title.contains(query, ignoreCase = true) ||
                note.content.contains(query, ignoreCase = true)
        }
    }

    fun openNewNote() {
        editorNote = null
        showDeleteConfirmation = false
        showEditor = true
    }

    fun openNote(note: Note) {
        editorNote = note
        showDeleteConfirmation = false
        showEditor = true
    }

    fun saveNote(title: String, content: String) {
        val trimmedTitle = title.trim()
        val trimmedContent = content.trim()
        if (trimmedTitle.isEmpty() && trimmedContent.isEmpty()) {
            showEditor = false
            return
        }
        val fallbackTitle = trimmedTitle.ifEmpty {
            trimmedContent.lineSequence().firstOrNull()?.take(24) ?: "無題のメモ"
        }
        val updated = Note(
            id = editorNote?.id ?: System.currentTimeMillis(),
            title = fallbackTitle,
            content = trimmedContent,
            updatedAt = System.currentTimeMillis()
        )
        notes = (notes.filterNot { it.id == updated.id } + updated)
            .sortedByDescending(Note::updatedAt)
        store.save(notes)
        showEditor = false
    }

    fun deleteNote() {
        val id = editorNote?.id ?: return
        notes = notes.filterNot { it.id == id }
        store.save(notes)
        showDeleteConfirmation = false
        showEditor = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Notes, contentDescription = null)
                        Spacer(Modifier.width(10.dp))
                        Text("メモ帳", fontWeight = FontWeight.Bold)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = ::openNewNote) {
                Icon(Icons.Default.Add, contentDescription = "新しいメモ")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                placeholder = { Text("メモを検索") },
                shape = RoundedCornerShape(16.dp)
            )
            Spacer(Modifier.height(16.dp))

            if (filteredNotes.isEmpty()) {
                EmptyState(hasSearch = searchText.isNotBlank(), onCreate = ::openNewNote)
            } else {
                Text(
                    text = if (searchText.isBlank()) "すべてのメモ" else "検索結果 ${filteredNotes.size}件",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredNotes, key = Note::id) { note ->
                        NoteCard(note = note, onClick = { openNote(note) })
                    }
                }
            }
        }
    }

    if (showEditor) {
        NoteEditorDialog(
            note = editorNote,
            onDismiss = { showEditor = false },
            onSave = ::saveNote,
            onDelete = if (editorNote == null) null else ({ showDeleteConfirmation = true })
        )
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("メモを削除しますか？") },
            text = { Text("この操作は取り消せません。") },
            confirmButton = {
                TextButton(onClick = ::deleteNote) { Text("削除") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) { Text("キャンセル") }
            }
        )
    }
}

@Composable
private fun NoteCard(note: Note, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, top = 15.dp, end = 8.dp, bottom = 15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = note.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    text = note.content.ifEmpty { "本文なし" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(7.dp))
                Text(
                    text = formatDate(note.updatedAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onClick) {
                Icon(Icons.Default.Edit, contentDescription = "編集")
            }
        }
    }
}

@Composable
private fun EmptyState(hasSearch: Boolean, onCreate: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                modifier = Modifier.size(72.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Notes,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = if (hasSearch) "該当するメモがありません" else "メモはまだありません",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            if (!hasSearch) {
                Spacer(Modifier.height(6.dp))
                Text("右下の＋からメモを作成できます。")
                Spacer(Modifier.height(16.dp))
                TextButton(onClick = onCreate) { Text("メモを作成") }
            }
        }
    }
}

@Composable
private fun NoteEditorDialog(
    note: Note?,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
    onDelete: (() -> Unit)?
) {
    var title by remember(note?.id) { mutableStateOf(note?.title.orEmpty()) }
    var content by remember(note?.id) { mutableStateOf(note?.content.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (note == null) "新しいメモ" else "メモを編集") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("タイトル") },
                    placeholder = { Text("タイトルを入力") }
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    label = { Text("本文") },
                    placeholder = { Text("内容を入力") }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(title, content) }) { Text("保存") }
        },
        dismissButton = {
            Row {
                if (onDelete != null) {
                    TextButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("削除")
                    }
                }
                TextButton(onClick = onDismiss) { Text("キャンセル") }
            }
        }
    )
}

private fun formatDate(timestamp: Long): String =
    SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.JAPAN).format(Date(timestamp))
