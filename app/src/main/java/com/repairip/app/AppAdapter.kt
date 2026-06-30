package com.repairip.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AppAdapter(
    private val items: MutableList<AppInfo>,
    private val onTap: (AppInfo) -> Unit
) : RecyclerView.Adapter<AppAdapter.VH>() {

    override fun onCreateViewHolder(p: ViewGroup, vt: Int): VH {
        val v = LayoutInflater.from(p.context).inflate(R.layout.item_app, p, false)
        return VH(v)
    }

    override fun onBindViewHolder(h: VH, i: Int) {
        val a = items[i]
        h.ivIcon.setImageDrawable(a.icon)
        h.tvName.text = a.name
        h.tvPkg.text = a.pkg
        h.tvBadge.visibility = if (a.hasPairIP) View.VISIBLE else View.GONE
        h.itemView.setOnClickListener { onTap(a) }
    }

    override fun getItemCount() = items.size

    fun update(list: List<AppInfo>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val ivIcon: ImageView = v.findViewById(R.id.ivIcon)
        val tvName: TextView = v.findViewById(R.id.tvName)
        val tvPkg: TextView = v.findViewById(R.id.tvPackage)
        val tvBadge: TextView = v.findViewById(R.id.tvBadge)
    }
}
