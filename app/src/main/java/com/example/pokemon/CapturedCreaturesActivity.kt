package com.example.pokemon

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.pokemon.data.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CapturedCreaturesActivity : BaseActivity() {

    private lateinit var db: AppDatabase
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: CreatureAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_captured_creatures)

        recyclerView = findViewById(R.id.rvCreatures)
        recyclerView.layoutManager = LinearLayoutManager(this)

        db = AppDatabase.getDatabase(this)


        lifecycleScope.launch {
            val creatures = withContext(Dispatchers.IO) {
                db.creatureDao().getAll()
            }

            //switch back to Main thread before setting adapter
            adapter = CreatureAdapter(creatures)
            recyclerView.adapter = adapter
        }
    }
}
