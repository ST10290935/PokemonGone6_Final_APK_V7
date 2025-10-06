package com.example.pokemon

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class EncyclopediaAdapter(private val items: List<CreatureEntry>) :
    RecyclerView.Adapter<EncyclopediaAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgCreature: ImageView = view.findViewById(R.id.imgCreature)
        val txtName: TextView = view.findViewById(R.id.txtCreatureName)
        val txtDesc: TextView = view.findViewById(R.id.txtCreatureDescription)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_creature_entry, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val creature = items[position]
        holder.imgCreature.setImageResource(creature.imageRes)
        holder.txtName.text = creature.name
        holder.txtDesc.text = creature.description
    }
}
