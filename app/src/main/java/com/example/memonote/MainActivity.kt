package com.example.memonote

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // ナビゲーションバーをアプリの上に重ねず、常に操作できるようにする。
        WindowCompat.setDecorFitsSystemWindows(window, true)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
            show(WindowInsetsCompat.Type.navigationBars())
        }
        val store = NoteStore(applicationContext)
        setContent { MemoTheme { MemoApp(store) } }
    }
}

@Composable
private fun MemoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = androidx.compose.material3.lightColorScheme(
            primary = Color(0xFF4F5D95),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFDCE2FF),
            background = Color(0xFFF9F9FF),
            surface = Color(0xFFF9F9FF)
        ),
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MemoApp(store: NoteStore) {
    val context = LocalContext.current
    var notes by remember { mutableStateOf(store.load()) }
    var searchText by rememberSaveable { mutableStateOf("") }
    var editorNote by remember { mutableStateOf<Note?>(null) }
    var showEditor by rememberSaveable { mutableStateOf(false) }
    var showDeleteConfirmation by rememberSaveable { mutableStateOf(false) }
    var showTrash by rememberSaveable { mutableStateOf(false) }
    var sortByTitle by rememberSaveable { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(store.exportJson()) }
                ?: error("保存先を開けませんでした")
        }.onSuccess {
            Toast.makeText(context, "バックアップを保存しました", Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(context, "バックアップの保存に失敗しました", Toast.LENGTH_SHORT).show()
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                ?: error("ファイルを開けませんでした")
        }.mapCatching { store.importJson(it) }
            .onSuccess {
                notes = it
                Toast.makeText(context, "${it.size}件のメモを復元しました", Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(context, "復元できないファイルです", Toast.LENGTH_SHORT).show()
            }
    }

    val activeNotes = notes.filter { it.deletedAt == null }
    val filteredNotes = remember(activeNotes, searchText) {
        val query = searchText.trim()
        if (query.isEmpty()) activeNotes
        else activeNotes.filter { note ->
            note.title.contains(query, ignoreCase = true) ||
                note.content.contains(query, ignoreCase = true)
        }
    }
    val trashedNotes = notes.filter { it.deletedAt != null }.sortedByDescending { it.deletedAt }
    val displayedNotes = if (sortByTitle) {
        filteredNotes.sortedWith(compareBy<Note> { !it.isPinned }.thenBy { it.title.lowercase(Locale.JAPAN) })
    } else {
        filteredNotes.sortedForDisplay()
    }

    fun persist(updatedNotes: List<Note>) {
        notes = updatedNotes.sortedForDisplay()
        store.save(notes)
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

    fun upsertNote(title: String, content: String) {
        val trimmedTitle = title.trim()
        val trimmedContent = content.trim()
        if (trimmedTitle.isEmpty() && trimmedContent.isEmpty()) return
        val previous = editorNote
        val updated = Note(
            id = previous?.id ?: System.currentTimeMillis(),
            title = trimmedTitle.ifEmpty {
                trimmedContent.lineSequence().firstOrNull()?.take(24) ?: "無題のメモ"
            },
            content = trimmedContent,
            updatedAt = System.currentTimeMillis(),
            isPinned = previous?.isPinned ?: false
        )
        persist(notes.filterNot { it.id == updated.id } + updated)
        editorNote = updated
    }

    fun moveToTrash() {
        val id = editorNote?.id ?: return
        persist(notes.map { note ->
            if (note.id == id) note.copy(deletedAt = System.currentTimeMillis()) else note
        })
        showDeleteConfirmation = false
        showEditor = false
    }

    fun togglePin(note: Note) {
        persist(notes.map { current ->
            if (current.id == note.id) current.copy(isPinned = !current.isPinned) else current
        })
    }

    fun restore(note: Note) {
        persist(notes.map { current ->
            if (current.id == note.id) current.copy(deletedAt = null, updatedAt = System.currentTimeMillis()) else current
        })
    }

    fun permanentlyDelete(note: Note) {
        persist(notes.filterNot { it.id == note.id })
    }

    fun duplicateNote(note: Note) {
        val copied = note.copy(
            id = System.currentTimeMillis(),
            title = "${note.title}（コピー）",
            updatedAt = System.currentTimeMillis(),
            isPinned = false,
            deletedAt = null
        )
        persist(notes + copied)
        editorNote = copied
    }

    fun shareNote(note: Note) {
        val text = buildString {
            append(note.title)
            if (note.content.isNotBlank()) append("\n\n${note.content}")
        }
        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TITLE, note.title)
            putExtra(Intent.EXTRA_TEXT, text)
        }, "メモを共有"))
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Notes, contentDescription = null)
                        Spacer(Modifier.width(10.dp))
                        Text("メモ帳", fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    TextButton(onClick = { exportLauncher.launch("MemoNote-backup.json") }) { Text("保存") }
                    TextButton(onClick = { importLauncher.launch(arrayOf("application/json", "text/plain")) }) { Text("復元") }
                    IconButton(onClick = { showTrash = true }) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "ゴミ箱")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = ::openNewNote,
                modifier = Modifier.navigationBarsPadding()
            ) {
                Icon(Icons.Default.Add, contentDescription = "新しいメモ")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp)
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

            if (displayedNotes.isEmpty()) {
                EmptyState(hasSearch = searchText.isNotBlank(), onCreate = ::openNewNote)
            } else {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (searchText.isBlank()) "すべてのメモ" else "検索結果 ${displayedNotes.size}件",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { sortByTitle = !sortByTitle }) {
                        Text(if (sortByTitle) "更新順" else "名前順")
                    }
                }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 112.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(displayedNotes, key = Note::id) { note ->
                        NoteCard(note, onClick = { openNote(note) }, onTogglePin = { togglePin(note) })
                    }
                }
            }
        }
    }

    if (showEditor) {
        NoteEditorDialog(
            note = editorNote,
            onDismiss = { showEditor = false },
            onSave = { title, content -> upsertNote(title, content); showEditor = false },
            onAutoSave = ::upsertNote,
            onDelete = if (editorNote == null) null else ({ showDeleteConfirmation = true }),
            onShare = editorNote?.let { { shareNote(it) } },
            onDuplicate = editorNote?.let { { duplicateNote(it) } }
        )
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("ゴミ箱へ移動しますか？") },
            text = { Text("ゴミ箱から復元できます。") },
            confirmButton = { TextButton(onClick = ::moveToTrash) { Text("移動") } },
            dismissButton = { TextButton(onClick = { showDeleteConfirmation = false }) { Text("キャンセル") } }
        )
    }

    if (showTrash) {
        TrashDialog(
            notes = trashedNotes,
            onDismiss = { showTrash = false },
            onRestore = ::restore,
            onPermanentlyDelete = ::permanentlyDelete
        )
    }
}

@Composable
private fun NoteCard(note: Note, onClick: () -> Unit, onTogglePin: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 18.dp, top = 15.dp, end = 8.dp, bottom = 15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(note.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(5.dp))
                Text(notePreview(note.content), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(7.dp))
                Text(formatDate(note.updatedAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onTogglePin) {
                Icon(Icons.Default.PushPin, contentDescription = "ピン留め", tint = if (note.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onClick) { Icon(Icons.Default.Edit, contentDescription = "編集") }
        }
    }
}

@Composable
private fun EmptyState(hasSearch: Boolean, onCreate: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(modifier = Modifier.size(72.dp), shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Notes, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp)) }
            }
            Spacer(Modifier.height(16.dp))
            Text(if (hasSearch) "該当するメモがありません" else "メモはまだありません", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
    onAutoSave: (String, String) -> Unit,
    onDelete: (() -> Unit)?,
    onShare: (() -> Unit)?,
    onDuplicate: (() -> Unit)?
) {
    val context = LocalContext.current
    var title by remember(note?.id) { mutableStateOf(note?.title.orEmpty()) }
    var content by remember(note?.id) { mutableStateOf(note?.content.orEmpty()) }
    val checklistItems = remember(content) { parseChecklistItems(content) }
    val voiceInputLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val recognizedText = if (result.resultCode == Activity.RESULT_OK) {
            result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        } else {
            null
        }
        if (!recognizedText.isNullOrBlank()) {
            content = content.trimEnd() + if (content.isBlank()) recognizedText else "\n$recognizedText"
        }
    }

    LaunchedEffect(title, content) {
        if (title == note?.title.orEmpty() && content == note?.content.orEmpty()) return@LaunchedEffect
        if (title.isBlank() && content.isBlank()) return@LaunchedEffect
        delay(700)
        onAutoSave(title, content)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (note == null) "新しいメモ" else "メモを編集") },
        text = {
            Column {
                OutlinedTextField(value = title, onValueChange = { title = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("タイトル") }, placeholder = { Text("タイトルを入力") })
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(value = content, onValueChange = { content = it }, modifier = Modifier.fillMaxWidth().height(160.dp), label = { Text("本文") }, placeholder = { Text("内容を入力") })
                Row {
                    TextButton(onClick = {
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.JAPAN.toLanguageTag())
                            putExtra(RecognizerIntent.EXTRA_PROMPT, "話してください")
                        }
                        runCatching { voiceInputLauncher.launch(intent) }
                            .onFailure { Toast.makeText(context, "音声入力を開始できません", Toast.LENGTH_SHORT).show() }
                    }) { Text("🎤 音声入力") }
                    TextButton(onClick = {
                        content = content.trimEnd() + if (content.isBlank()) "- [ ] " else "\n- [ ] "
                    }) { Text("＋ チェック項目") }
                }
                if (checklistItems.isNotEmpty()) {
                    HorizontalDivider()
                    Text("チェックリスト", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                    checklistItems.forEach { item ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = item.checked, onCheckedChange = { checked -> content = replaceChecklistItem(content, item.lineIndex, checked) })
                            Text(item.text.ifBlank { "項目を入力" })
                        }
                    }
                }
                Text("入力内容は約1秒後に自動保存されます。", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
            }
        },
        confirmButton = { TextButton(onClick = { onSave(title, content) }) { Text("閉じる") } },
        dismissButton = {
            Row {
                if (onShare != null) TextButton(onClick = onShare) { Text("共有") }
                if (onDuplicate != null) TextButton(onClick = onDuplicate) { Text("複製") }
                if (onDelete != null) {
                    TextButton(onClick = onDelete) { Icon(Icons.Default.Delete, null); Spacer(Modifier.width(4.dp)); Text("削除") }
                }
                TextButton(onClick = onDismiss) { Text("キャンセル") }
            }
        }
    )
}

@Composable
private fun TrashDialog(notes: List<Note>, onDismiss: () -> Unit, onRestore: (Note) -> Unit, onPermanentlyDelete: (Note) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ゴミ箱") },
        text = {
            if (notes.isEmpty()) Text("ゴミ箱は空です。")
            else Column(modifier = Modifier.height(260.dp)) {
                notes.forEach { note ->
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(note.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(formatDate(note.deletedAt ?: note.updatedAt), style = MaterialTheme.typography.labelSmall)
                        }
                        IconButton(onClick = { onRestore(note) }) { Icon(Icons.Default.RestoreFromTrash, contentDescription = "復元") }
                        IconButton(onClick = { onPermanentlyDelete(note) }) { Icon(Icons.Default.Delete, contentDescription = "完全に削除") }
                    }
                    HorizontalDivider()
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("閉じる") } }
    )
}

private data class ChecklistItem(val lineIndex: Int, val checked: Boolean, val text: String)

private fun parseChecklistItems(content: String): List<ChecklistItem> {
    val expression = Regex("^\\s*[-*]\\s*\\[([ xX])\\]\\s*(.*)$")
    return content.lines().mapIndexedNotNull { index, line ->
        expression.matchEntire(line)?.let { match -> ChecklistItem(index, match.groupValues[1].equals("x", true), match.groupValues[2]) }
    }
}

private fun replaceChecklistItem(content: String, lineIndex: Int, checked: Boolean): String =
    content.lines().mapIndexed { index, line ->
        if (index == lineIndex) line.replace(Regex("\\[([ xX])\\]"), if (checked) "[x]" else "[ ]") else line
    }.joinToString("\n")

private fun notePreview(content: String): String =
    content.ifBlank { "本文なし" }.replace("[ ]", "☐").replace("[x]", "☑").replace("[X]", "☑")

private fun formatDate(timestamp: Long): String =
    SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.JAPAN).format(Date(timestamp))
