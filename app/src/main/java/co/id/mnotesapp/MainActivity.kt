package co.id.mnotesapp

import android.content.Intent
import android.database.ContentObserver
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import co.id.mnotesapp.adapter.NoteAdapter
import co.id.mnotesapp.db.DatabaseContract.NoteColumns.Companion.CONTENT_URI
import co.id.mnotesapp.db.NoteHelper
import co.id.mnotesapp.entity.Note
import co.id.mnotesapp.helper.MappingHelper
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var adapter: NoteAdapter
    private lateinit var noteHelper: NoteHelper

    companion object {
        private const val EXTRA_STATE = "EXTRA_STATE"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        supportActionBar?.title = "Notes"
        rv_notes.layoutManager = LinearLayoutManager(this)
        rv_notes.setHasFixedSize(true)
        adapter = NoteAdapter(this)
        rv_notes.adapter = adapter

        fab_add.setOnClickListener {
            val intent = Intent(this@MainActivity, NoteAddUpdateActivity::class.java)
            startActivityForResult(intent, NoteAddUpdateActivity.REQUEST_ADD)
        }

        val handlerThread = HandlerThread("DataObserver")
        handlerThread.start()
        val handler = Handler(handlerThread.looper)

        val myObserver = object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean) {
                loadNotesAsync()
            }
        }

        contentResolver.registerContentObserver(CONTENT_URI, true, myObserver)

        noteHelper = NoteHelper.getInstance(applicationContext)
        noteHelper.open()

        if (savedInstanceState == null) {
            loadNotesAsync()
        } else {
            val list = savedInstanceState.getParcelableArrayList<Note>(EXTRA_STATE)
            if (list != null) {
                adapter.listNotes = list
            }
        }
    }

    private fun loadNotesAsync() {
        GlobalScope.launch(Dispatchers.Main) {
            progressbar.visibility = View.VISIBLE
            val deferredNotes = async(Dispatchers.IO) {
                val cursor = contentResolver?.query(CONTENT_URI, null, null, null, null)
                MappingHelper.mapCursorToArrayList(cursor)
            }
            progressbar.visibility = View.INVISIBLE
            val notes = deferredNotes.await()
            if (notes.size > 0) {
                adapter.listNotes = notes
            } else {
                adapter.listNotes = ArrayList()
                showSnackbarMessage("Tidak ada data saat ini")
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelableArrayList(EXTRA_STATE, adapter.listNotes)
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (data != null) {
            when (requestCode) {
                NoteAddUpdateActivity.REQUEST_ADD -> if (resultCode == NoteAddUpdateActivity.RESULT_ADD) {
                    val note = data.getParcelableExtra<Note>(NoteAddUpdateActivity.EXTRA_NOTE)

                    adapter.addItem(note)
                    rv_notes.smoothScrollToPosition(adapter.itemCount - 1)

                    showSnackbarMessage("Satu item berhasil ditambahkan")
                }
                NoteAddUpdateActivity.REQUEST_UPDATE ->
                    when (resultCode) {
                        NoteAddUpdateActivity.RESULT_UPDATE -> {
                            val note = data.getParcelableExtra<Note>(NoteAddUpdateActivity.EXTRA_NOTE)
                            val position = data.getIntExtra(NoteAddUpdateActivity.EXTRA_POSITION, 0)

                            adapter.updateItem(position, note)
                            rv_notes.smoothScrollToPosition(position)

                            showSnackbarMessage("Satu item berhasil diubah")
                        }
                        NoteAddUpdateActivity.RESULT_DELETE -> {
                            val position = data.getIntExtra(NoteAddUpdateActivity.EXTRA_POSITION, 0)

                            adapter.removeItem(position)
                            showSnackbarMessage("Satu item berhasil dihapus")
                        }
                    }
            }
        }
    }
    private fun showSnackbarMessage(message: String) {
        Snackbar.make(rv_notes, message, Snackbar.LENGTH_SHORT).show()
    }
    override fun onDestroy() {
        super.onDestroy()
        noteHelper.close()
    }
}



