package com.recs.sunq.inspection.optionsMenu

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
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
    private lateinit var adapter: AlarmAdapter
    private lateinit var progressBar: ProgressBar

    private val refreshReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d("AlarmFragment", "Broadcast received, refreshing list")
            // 리스트를 새로고침합니다.
            AlarmList(requireContext())
            Toast.makeText(requireContext(), "알람 리스트가 새로고침되었습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_alarm, container, false)

        listView = view.findViewById(R.id.alarm_list)
        progressBar = view.findViewById(R.id.progress_bar) // ProgressBar 추가

        // 리스트를 초기화합니다.
        AlarmList(requireContext())

        // BroadcastReceiver를 등록합니다.
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(
            refreshReceiver, IntentFilter("com.example.app.ACTION_REFRESH_ALARM_LIST")
        )
        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // BroadcastReceiver를 해제합니다.
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(refreshReceiver)
    }

    private fun AlarmList(context: Context) {
        val tokenManager = TokenManager(context)
        val userSeq = tokenManager.getUserseq() ?: ""
        val apiService = RetrofitInstance.createApi(context)
        val appName = "WatchQ"

        progressBar.visibility = View.VISIBLE // ProgressBar 표시
        Log.d("AlarmFragment", "Fetching alarm list")

        apiService.selectAlertList(userSeq, appName).enqueue(object : Callback<List<Alarm>> {
            override fun onResponse(call: Call<List<Alarm>>, response: Response<List<Alarm>>) {
                progressBar.visibility = View.GONE // ProgressBar 숨기기
                if (response.isSuccessful) {
                    val alertList = response.body() ?: listOf()

                    // 서버 응답으로부터 받은 알람 목록을 사용합니다.
                    adapter = AlarmAdapter(requireContext(), alertList.toMutableList())
                    listView.adapter = adapter

                    Log.d("AlarmFragment", "Alarm list refreshed with ${alertList.size} items")
                } else {
                    Log.e("AlarmList", "Response not successful: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<List<Alarm>>, t: Throwable) {
                progressBar.visibility = View.GONE // ProgressBar 숨기기
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
            val appName = "WatchQ"
            val readMessage = ReadMessage(userSeq, alarmSeq, appName)

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