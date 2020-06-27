package com.rolandvitezhu.todocloud.ui.activity.main.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import butterknife.BindView
import butterknife.ButterKnife
import com.rolandvitezhu.todocloud.R
import com.rolandvitezhu.todocloud.data.PredefinedList
import com.rolandvitezhu.todocloud.di.FragmentScope
import java.util.*
import javax.inject.Inject

@FragmentScope
class PredefinedListAdapter @Inject constructor() : BaseAdapter() {
    private val predefinedLists: MutableList<PredefinedList>

    @BindView(R.id.textview_predefinedlist_actiontext)
    lateinit var tvTitle: TextView

    @BindView(R.id.imageview_predefinedlist)
    lateinit var ivPredefinedList: ImageView
    override fun getCount(): Int {
        return predefinedLists.size
    }

    override fun getItem(position: Int): Any {
        return predefinedLists[position]
    }

    override fun getItemId(position: Int): Long {
        return 0
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val (title) = predefinedLists[position]
        val layoutInflater = parent.context.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE
        ) as LayoutInflater
        val itemView = layoutInflater.inflate(R.layout.item_predefinedlist, null)
        ButterKnife.bind(this, itemView)
        when (position) {
            0 -> ivPredefinedList.setImageResource(R.drawable.baseline_today_24)
            1 -> ivPredefinedList.setImageResource(R.drawable.baseline_view_week_24)
            2 -> ivPredefinedList.setImageResource(R.drawable.baseline_all_inclusive_24)
            3 -> ivPredefinedList.setImageResource(R.drawable.baseline_done_24)
        }
        tvTitle.text = title
        return itemView
    }

    fun update(predefinedLists: List<PredefinedList>?) {
        this.predefinedLists.clear()
        this.predefinedLists.addAll(predefinedLists!!)
    }

    fun clear() {
        predefinedLists.clear()
        notifyDataSetChanged()
    }

    init {
        predefinedLists = ArrayList()
    }
}