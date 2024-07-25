package com.recs.sunq.inspection

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import android.widget.BaseExpandableListAdapter
import android.widget.ExpandableListView
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat

class ExpandableListAdapter(
    private val context: Context,
    private val listDataHeader: List<String>,
    private val listDataChild: HashMap<String, List<String>> // 이미지 리소스 제거
) : BaseExpandableListAdapter() {

    private var selectedGroupPosition: Int = -1

    override fun getChild(groupPosition: Int, childPosition: Int): Any {
        return listDataChild[listDataHeader[groupPosition]]!![childPosition]
    }

    override fun getChildId(groupPosition: Int, childPosition: Int): Long {
        return childPosition.toLong()
    }

    override fun getGroupCount(): Int {
        return listDataHeader.size
    }

    override fun getChildrenCount(groupPosition: Int): Int {
        return listDataChild[listDataHeader[groupPosition]]?.size ?: 0
    }

    override fun getGroup(groupPosition: Int): Any {
        return listDataHeader[groupPosition]
    }

    override fun getGroupId(groupPosition: Int): Long {
        return groupPosition.toLong()
    }

    override fun hasStableIds(): Boolean {
        return false
    }

    override fun isChildSelectable(groupPosition: Int, childPosition: Int): Boolean {
        return true
    }

    override fun getChildView(
        groupPosition: Int,
        childPosition: Int,
        isLastChild: Boolean,
        convertView: View?,
        parent: ViewGroup?
    ): View {
        val view: View
        val viewHolder: ChildViewHolder

        if (convertView == null) {
            val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            view = inflater.inflate(R.layout.list_item, parent, false)
            viewHolder = ChildViewHolder(view)
            view.tag = viewHolder
        } else {
            view = convertView
            viewHolder = view.tag as ChildViewHolder
        }

        val childData = getChild(groupPosition, childPosition) as String
        viewHolder.txtListChild.text = childData
        viewHolder.txtListChild.setTextColor(ContextCompat.getColor(context, android.R.color.white))

        return view
    }

    override fun getGroupView(
        groupPosition: Int,
        isExpanded: Boolean,
        convertView: View?,
        parent: ViewGroup?
    ): View {
        val view: View
        val viewHolder: GroupViewHolder

        if (convertView == null) {
            val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            view = inflater.inflate(R.layout.group_list_item, parent, false)
            viewHolder = GroupViewHolder(view)
            view.tag = viewHolder
        } else {
            view = convertView
            viewHolder = view.tag as GroupViewHolder
        }

        viewHolder.lblListHeader.text = getGroup(groupPosition) as String
        viewHolder.indicator.rotation = if (isExpanded) 180f else 0f

        viewHolder.lblListHeader.setTextColor(
            if (groupPosition == selectedGroupPosition && isExpanded)
                ContextCompat.getColor(context, android.R.color.holo_red_light)
            else
                ContextCompat.getColor(context, android.R.color.white)
        )
        viewHolder.indicator.tag = groupPosition
        return view
    }

    fun setSelectedPosition(groupPosition: Int, isExpanded: Boolean, listView: ExpandableListView) {
        selectedGroupPosition = if (isExpanded) groupPosition else -1
        notifyDataSetChanged()

        listView.postOnAnimation {
            val firstVisiblePosition = listView.firstVisiblePosition
            val lastVisiblePosition = listView.lastVisiblePosition

            for (i in firstVisiblePosition..lastVisiblePosition) {
                val groupView = listView.getChildAt(i - firstVisiblePosition)
                val indicator: ImageView? = groupView?.findViewById(R.id.groupIndicator)
                if (indicator != null && (i == groupPosition || i == selectedGroupPosition)) {
                    indicator.clearAnimation()

                    val fromDegrees = if (isExpanded) -180f else 180f
                    val toDegrees = if (isExpanded) 0f else 0f

                    val rotateAnimation = RotateAnimation(
                        fromDegrees, toDegrees,
                        Animation.RELATIVE_TO_SELF, 0.5f,
                        Animation.RELATIVE_TO_SELF, 0.5f
                    ).apply {
                        duration = 250
                        fillAfter = true
                    }

                    indicator.startAnimation(rotateAnimation)
                }
            }
        }
    }

    private class ChildViewHolder(view: View) {
        // 이미지를 제거하고 텍스트만 사용
        val txtListChild: TextView = view.findViewById(R.id.child_text)
    }

    private class GroupViewHolder(view: View) {
        val lblListHeader: TextView = view.findViewById(R.id.lblListHeader)
        val indicator: ImageView = view.findViewById(R.id.groupIndicator)
    }
}
