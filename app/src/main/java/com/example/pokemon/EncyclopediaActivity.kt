package com.example.pokemon

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class EncyclopediaActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_encyclopedia)

        val recyclerView = findViewById<RecyclerView>(R.id.rvEncyclopedia)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val creatures = listOf(
            CreatureEntry("Fluffy", R.drawable.creature1, "Type: Fire\nHP: 80\nAttack: 50\nSpeed: 60"),
            CreatureEntry("Sparky", R.drawable.creature2, "Type: Electric\nHP: 70\nAttack: 60\nSpeed: 90"),
            CreatureEntry("Leafy", R.drawable.creature3, "Type: Grass\nHP: 90\nAttack: 40\nSpeed: 50"),
            CreatureEntry("Aqua", R.drawable.creature4, "Type: Water\nHP: 85\nAttack: 55\nSpeed: 65"),
            CreatureEntry("Rocko", R.drawable.creature5, "Type: Rock\nHP: 100\nAttack: 70\nSpeed: 40"),
            CreatureEntry("Shadow", R.drawable.creature6, "Type: Dark\nHP: 75\nAttack: 80\nSpeed: 85"),
            CreatureEntry("Frosty", R.drawable.creature7, "Type: Ice\nHP: 60\nAttack: 45\nSpeed: 95"),
            CreatureEntry("Blaze", R.drawable.creature8, "Type: Fire\nHP: 88\nAttack: 78\nSpeed: 70"),
            CreatureEntry("Stormy", R.drawable.creature9, "Type: Electric\nHP: 72\nAttack: 66\nSpeed: 92"),
            CreatureEntry("Petal", R.drawable.creature10, "Type: Grass\nHP: 95\nAttack: 52\nSpeed: 55"),
            CreatureEntry("Bubble", R.drawable.creature11, "Type: Water\nHP: 82\nAttack: 48\nSpeed: 68"),
            CreatureEntry("Boulder", R.drawable.creature12, "Type: Rock\nHP: 110\nAttack: 85\nSpeed: 35"),
            CreatureEntry("Nightfang", R.drawable.creature13, "Type: Dark\nHP: 78\nAttack: 88\nSpeed: 80"),
            CreatureEntry("Glacier", R.drawable.creature14, "Type: Ice\nHP: 65\nAttack: 50\nSpeed: 98"),
            CreatureEntry("Inferno", R.drawable.creature15, "Type: Fire\nHP: 92\nAttack: 85\nSpeed: 72"),
            CreatureEntry("Volt", R.drawable.creature16, "Type: Electric\nHP: 76\nAttack: 70\nSpeed: 89")

        )

        val adapter = EncyclopediaAdapter(creatures)
        recyclerView.adapter = adapter
    }
}

data class CreatureEntry(
    val name: String,
    val imageRes: Int,
    val description: String
)
