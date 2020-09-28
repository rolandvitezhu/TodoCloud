package com.rolandvitezhu.todocloud.ui.activity.main.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import com.rolandvitezhu.todocloud.data.PredefinedList
import com.rolandvitezhu.todocloud.databinding.ItemPredefinedlistBinding
import com.rolandvitezhu.todocloud.di.FragmentScope
import java.util.*
import javax.inject.Inject

@FragmentScope
class PredefinedListAdapter @Inject constructor() : BaseAdapter() {

    val predefinedLists: MutableList<PredefinedList>

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
            itemPredefinedlistBinding = ItemPredefinedlistBinding.inflate(
                    layoutInflater, parent, false
            )
            convertView = itemPredefinedlistBinding.root
        } else {
            itemPredefinedlistBinding = convertView.tag as ItemPredefinedlistBinding
        }

        itemPredefinedlistBinding.predefinedListAdapter = this
        itemPredefinedlistBinding.predefinedList = predefinedLists[position]
        itemPredefinedlistBinding.executePendingBindings()

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