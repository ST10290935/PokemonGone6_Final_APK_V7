package com.example.pokemon

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.pokemon.data.Creature

class CreatureAdapter(private var creatures: List<Creature>) :
    RecyclerView.Adapter<CreatureAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgCreature: ImageView = itemView.findViewById(R.id.imgCreature)
        val txtName: TextView = itemView.findViewById(R.id.txtName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_creature, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val creature = creatures[position]
        holder.txtName.text = creature.name

        // Safe drawable loading
        val context = holder.imgCreature.context
        val drawable = try {
            context.resources.getDrawable(creature.sprite, context.theme)
        } catch (e: Exception) {
            context.getDrawable(R.drawable.default_creature) // fallback drawable
        }
        holder.imgCreature.setImageDrawable(drawable)
    }

    override fun getItemCount(): Int = creatures.size

    // New method to update data dynamically
    fun updateData(newList: List<Creature>) {
        creatures = newList
        notifyDataSetChanged()
    }
}
