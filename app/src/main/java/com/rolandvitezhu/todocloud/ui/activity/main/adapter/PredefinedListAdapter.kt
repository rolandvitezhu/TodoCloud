package com.rolandvitezhu.todocloud.ui.activity.main.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import androidx.databinding.DataBindingUtil
import com.rolandvitezhu.todocloud.R
import com.rolandvitezhu.todocloud.data.PredefinedList
import com.rolandvitezhu.todocloud.databinding.ItemPredefinedlistBinding
import com.rolandvitezhu.todocloud.di.FragmentScope
import java.util.*
import javax.inject.Inject

@FragmentScope
class PredefinedListAdapter @Inject constructor() : BaseAdapter() {

    private val predefinedLists: MutableList<PredefinedList>

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
        var convertView = convertView

        val itemPredefinedlistBinding: ItemPredefinedlistBinding
        val layoutInflater = parent.context.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE
        ) as LayoutInflater

        if (convertView == null) {
            itemPredefinedlistBinding = DataBindingUtil.inflate(
                    layoutInflater,
                    R.layout.item_predefinedlist,
                    parent,
                    false
            )
            convertView = itemPredefinedlistBinding.root
            itemPredefinedlistBinding.predefinedListAdapter = this
        } else {
            itemPredefinedlistBinding = convertView.tag as ItemPredefinedlistBinding
        }

        itemPredefinedlistBinding.textviewPredefinedlistActiontext.text = predefinedLists[position].title
        when (position) {
            0 -> itemPredefinedlistBinding.imageviewPredefinedlist.setImageResource(R.drawable.baseline_today_24)
            1 -> itemPredefinedlistBinding.imageviewPredefinedlist.setImageResource(R.drawable.baseline_view_week_24)
            2 -> itemPredefinedlistBinding.imageviewPredefinedlist.setImageResource(R.drawable.baseline_all_inclusive_24)
            3 -> itemPredefinedlistBinding.imageviewPredefinedlist.setImageResource(R.drawable.baseline_done_24)
        }
        convertView.tag = itemPredefinedlistBinding

        return convertView
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