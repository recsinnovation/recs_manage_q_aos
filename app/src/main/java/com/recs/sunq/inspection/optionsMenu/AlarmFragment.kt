package com.recs.sunq.inspection.optionsMenu

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import com.recs.sunq.androidapp.data.network.RetrofitInstance
import com.recs.sunq.inspection.R
import com.recs.sunq.inspection.data.TokenManager
import com.recs.sunq.inspection.data.model.Alarm
import com.recs.sunq.inspection.data.model.ReadMessage
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AlarmFragment : Fragment() {

    private lateinit var listView: ListView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_alarm, container, false)

        listView = view.findViewById(R.id.alarm_list)

        AlarmList(requireContext())

        return view
    }

    private fun AlarmList(context: Context) {
        val tokenManager = TokenManager(context)
        val userSeq = tokenManager.getUserseq() ?: ""
        val apiService = RetrofitInstance.createApi(context)

        apiService.selectAlertList(userSeq).enqueue(object : Callback<List<Alarm>> {
            override fun onResponse(call: Call<List<Alarm>>, response: Response<List<Alarm>>) {
                if (response.isSuccessful) {
                    val alertList = response.body() ?: listOf()

                    // 서버 응답으로부터 받은 알람 목록을 사용합니다.
                    val adapter = AlarmAdapter(requireContext(), alertList.toMutableList())
                    listView.adapter = adapter

                } else {
                    Log.e("AlarmList", "Response not successful: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<List<Alarm>>, t: Throwable) {
                // 에러 처리
                Log.e("AlarmList", "Error fetching alarm list", t)
            }
        })
    }

    private inner class AlarmAdapter(context: Context, var alarms: MutableList<Alarm>) :
        ArrayAdapter<Alarm>(context, 0, alarms) {

        override fun getCount(): Int = alarms.size

        override fun getItem(position: Int): Alarm? = alarms[position]

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val alarm = getItem(position)
            val view = convertView ?: LayoutInflater.from(context)
                .inflate(R.layout.list_item_alarm, parent, false)

            val titleView: TextView = view.findViewById(R.id.textView_alarm_title)
            val contentView: TextView = view.findViewById(R.id.textView_alarm_content)

            titleView.text = alarm?.title
            contentView.text = alarm?.content

            // is_read 값에 따라 글씨 색을 변경합니다.
            if (alarm?.is_read == "N") {
                titleView.setTextColor(Color.BLACK)
                contentView.setTextColor(Color.BLACK)
            } else {
                titleView.setTextColor(Color.GRAY)
                contentView.setTextColor(Color.GRAY)
            }

            view.setOnClickListener {
                alarm?.let { selectedAlarm ->
                    markAlarmAsRead(selectedAlarm)

                    selectedAlarm.is_read = "Y"
                    notifyDataSetChanged()

                    val bundle = bundleOf("url" to selectedAlarm.url)
                }
            }

            return view
        }

        private fun markAlarmAsRead(alarm: Alarm) {
            val tokenManager = TokenManager(context)
            val userSeq = tokenManager.getUserseq() ?: ""
            val alarmSeq = alarm.app_alarm_seq
            val readMessage = ReadMessage(userSeq, alarmSeq)

            Log.d("AlarmAdapter", "Sending ReadMessage: $readMessage")

            val apiService = RetrofitInstance.createApi(context)
            apiService.ReadAlert(readMessage).enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    if (response.isSuccessful) {
                        Log.d("AlarmAdapter", "Alarm marked as read")
                    } else {
                        Log.e(
                            "AlarmAdapter",
                            "Failed to mark alarm as read: ${response.errorBody()?.string()}"
                        )
                    }
                }

                override fun onFailure(call: Call<Void>, t: Throwable) {
                    Log.e("AlarmAdapter", "Error marking alarm as read", t)
                }
            })
        }
    }
}