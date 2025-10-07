//Code Attribution:
//For the code below these are the sources I have used to improve my knowledge and implement features:
//Android Knowledge, 2023. RecyclerView in Android Studio using Kotlin | Android Knowledge. [video online] Available at: <https://youtu.be/UDfyZLWyyVM?si=XkKwy4-9apD5AZcW> [Accessed 28 September 2025].
//Android Knowledge, 2023. RecyclerView in Fragment in Android Studio using Java | YouTube Clone. [video online] Available at: <https://youtu.be/R62YihuL4VI?si=oo1H04X-CO5oKFN8> [Accessed 27 September 2025].
//Foxandroid, 2022. Recyclerview in Fragment Android Studio Tutorial || Recyclerview || Fragment || Kotlin. [video online] Available at: <https://youtu.be/5mdV1hLbXzo?si=zTBFz3mHnEYuqx-Z> [Accessed 26 September 2025].
//LearningWorldsz, 2020. Java Android Room Database | Insert and Query | RecyclerView Example. [video online] Available at: <https://youtu.be/ONb_MuPBBlg?si=MdldmJ9DF-Y9T_pX> [Accessed 26 September 2025].


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
