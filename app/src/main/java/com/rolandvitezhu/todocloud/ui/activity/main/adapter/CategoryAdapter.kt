package com.rolandvitezhu.todocloud.ui.activity.main.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import androidx.databinding.DataBindingUtil
import com.rolandvitezhu.todocloud.R
import com.rolandvitezhu.todocloud.data.Category
import com.rolandvitezhu.todocloud.databinding.ItemCategoryBinding
import com.rolandvitezhu.todocloud.databinding.ItemListincategoryBinding
import com.rolandvitezhu.todocloud.di.FragmentScope
import kotlinx.android.synthetic.main.item_category.view.*
import java.util.*
import javax.inject.Inject

@FragmentScope
class CategoryAdapter @Inject constructor() : BaseExpandableListAdapter() {

    private val categories: MutableList<Category>
    private val lhmCategories: LinkedHashMap<Category, List<com.rolandvitezhu.todocloud.data.List>>

    override fun getGroupCount(): Int {
        return categories.size
    }

    override fun getChildrenCount(groupPosition: Int): Int {
        val category = categories[groupPosition]
        val lists = lhmCategories[category]
        return lists?.size ?: 0
    }

    override fun getGroup(groupPosition: Int): Any {
        return categories[groupPosition]
    }

    override fun getChild(groupPosition: Int, childPosition: Int): Any {
        val category = categories[groupPosition]
        val lists = lhmCategories[category]
        return lists?.get(childPosition)!!
    }

    override fun getGroupId(groupPosition: Int): Long {
        val (_id) = categories[groupPosition]
        return _id!!
    }

    override fun getChildId(groupPosition: Int, childPosition: Int): Long {
        val category = categories[groupPosition]
        val lists = lhmCategories[category]
        val list = lists?.get(childPosition)
        return (if (list != null) list._id else -1)!!
    }

    override fun hasStableIds(): Boolean {
        return false
    }

    override fun getGroupView(
            groupPosition: Int,
            isExpanded: Boolean,
            convertView: View?,
            parent: ViewGroup
    ): View {
        var convertView = convertView

        val layoutInflater = parent.context.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE
        ) as LayoutInflater
        val itemCategoryBinding: ItemCategoryBinding

        if (convertView == null) {
            itemCategoryBinding = DataBindingUtil.inflate(
                    layoutInflater,
                    R.layout.item_category,
                    parent,
                    false
            )
            convertView = itemCategoryBinding.root
            itemCategoryBinding.categoryAdapter = this
        } else {
            itemCategoryBinding = convertView.tag as ItemCategoryBinding
        }

        itemCategoryBinding.textviewItemcategoryActiontext.text = (getGroup(groupPosition) as Category).title
        convertView.tag = itemCategoryBinding
        handleCategoryIndicator(groupPosition, isExpanded, convertView)

        return convertView
    }

    private fun handleCategoryIndicator(groupPosition: Int, isExpanded: Boolean, convertView: View) {
        if (shouldNotShowGroupIndicator(groupPosition)) {
            hideExpandedGroupIndicator(convertView)
        } else if (isExpanded) {
            showExpandedGroupIndicator(convertView)
        } else {
            showCollapsedGroupIndicator(convertView)
        }
    }

    private fun hideExpandedGroupIndicator(convertView: View) {
        convertView.imageview_itemcategory_groupindicator.visibility = View.INVISIBLE
    }

    private fun showCollapsedGroupIndicator(convertView: View) {
        convertView.imageview_itemcategory_groupindicator.visibility = View.VISIBLE
        convertView.imageview_itemcategory_groupindicator.setImageResource(R.drawable.baseline_expand_less_24)
    }

    private fun showExpandedGroupIndicator(convertView: View) {
        convertView.imageview_itemcategory_groupindicator.visibility = View.VISIBLE
        convertView.imageview_itemcategory_groupindicator.setImageResource(R.drawable.baseline_expand_more_24)
    }

    private fun shouldNotShowGroupIndicator(groupPosition: Int): Boolean {
        return getChildrenCount(groupPosition) == 0
    }

    override fun getChildView(
            groupPosition: Int,
            childPosition: Int,
            isLastChild: Boolean,
            convertView: View?,
            parent: ViewGroup
    ): View {
        var convertView = convertView

        val itemListincategoryBinding: ItemListincategoryBinding
        val layoutInflater = parent.context.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE
        ) as LayoutInflater

        if (convertView == null) {
            itemListincategoryBinding = DataBindingUtil.inflate(
                    layoutInflater,
                    R.layout.item_listincategory,
                    parent,
                    false
            )
            convertView = itemListincategoryBinding.root
            itemListincategoryBinding.categoryAdapter = this
        } else {
            itemListincategoryBinding = convertView.tag as ItemListincategoryBinding
        }

        itemListincategoryBinding.textviewItemlistincategoryActiontext.text =
                (getChild(groupPosition, childPosition) as com.rolandvitezhu.todocloud.data.List).title
        convertView.tag = itemListincategoryBinding

        return convertView
    }

    override fun isChildSelectable(groupPosition: Int, childPosition: Int): Boolean {
        return true
    }

    fun update(lhmCategories: LinkedHashMap<Category, List<com.rolandvitezhu.todocloud.data.List>>) {
        categories.clear()
        this.lhmCategories.clear()
        categories.addAll(lhmCategories.keys)
        this.lhmCategories.putAll(lhmCategories)
    }

    fun clear() {
        categories.clear()
        lhmCategories.clear()
        notifyDataSetChanged()
    }

    init {
        categories = ArrayList()
        lhmCategories = LinkedHashMap()
    }
}