package com.recs.sunq.inspection

import android.animation.ValueAnimator
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ExpandableListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.navigation.NavigationView
import com.recs.sunq.inspection.Notification.AppMessagingService
import com.recs.sunq.inspection.databinding.ActivityMainBinding
import com.recs.sunq.inspection.ui.home.HomeFragment
import com.recs.sunq.inspection.ExpandableListAdapter
import com.recs.sunq.inspection.data.TokenManager

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    private lateinit var expandableListViewPlantList: ExpandableListView

    private lateinit var listDataHeaderPlantList: ArrayList<String>
    private lateinit var listDataChildPlantList: HashMap<String, List<String>>
    private lateinit var listAdapterPlantList: ExpandableListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarMain.toolbar)
        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        expandableListViewPlantList = navView.findViewById(R.id.plant_name_list)

        listDataHeaderPlantList = ArrayList()
        listDataChildPlantList = HashMap()

        prepareListData()

        setupExpandableListViews()

        listAdapterPlantList = ExpandableListAdapter(this, listDataHeaderPlantList, listDataChildPlantList)
        expandableListViewPlantList.setAdapter(listAdapterPlantList)

        expandableListViewPlantList.setOnGroupExpandListener { groupPosition ->
            Log.d("ExpandableListView", "Generation group $groupPosition expanded")
            collapseAllExcept(expandableListViewPlantList, groupPosition)
            setExpandableListViewHeight(expandableListViewPlantList, listAdapterPlantList)
            listAdapterPlantList.setSelectedPosition(groupPosition, true, expandableListViewPlantList)
        }

        expandableListViewPlantList.setOnGroupCollapseListener { groupPosition ->
            Log.d("ExpandableListView", "Generation group $groupPosition collapsed")
            setExpandableListViewHeight(expandableListViewPlantList, listAdapterPlantList)
            listAdapterPlantList.setSelectedPosition(groupPosition, false, expandableListViewPlantList)
        }

        expandableListViewPlantList.setOnChildClickListener { parent, v, groupPosition, childPosition, id ->
            val selectedItem = listAdapterPlantList.getChild(groupPosition, childPosition) as String
            val tokenManager = TokenManager(this)

            // TokenManager에서 plant_seq와 plant_name 업데이트
            val selectedPlant = tokenManager.getPlantList()?.find { it.plant_name == selectedItem }
            var url = "https://m.sunq.co.kr/device-management/inspection/history"
            if (selectedPlant != null) {
                tokenManager.updatePlantSeqAndName(selectedPlant.plant_seq, selectedPlant.plant_name)
            }

            val textView: TextView = findViewById(R.id.plantlist)
            textView.text = selectedItem

            updateUrl(url)
            drawerLayout.closeDrawers()
            false
        }

        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        setupNavigationListeners(navView, drawerLayout)

        val appMessagingService = AppMessagingService()
        appMessagingService.selectUserInfo(this)

        handleIntent(intent)
    }

    private fun setupNavigationListeners(
        navView: NavigationView,
        drawerLayout: DrawerLayout
    ) {
        navView.findViewById<TextView>(R.id.nav_main_screen).setOnClickListener {
            updateUrl("https://m.sunq.co.kr/device-management/inspection/history")
            drawerLayout.closeDrawers()
        }

        navView.findViewById<TextView>(R.id.nav_power_system).setOnClickListener {
            updateUrl("https://m.sunq.co.kr/device-management/inspection/regist")
            drawerLayout.closeDrawers()
        }

        navView.findViewById<TextView>(R.id.nav_problemhistory).setOnClickListener {
            updateUrl("https://m.sunq.co.kr/device-management/error-fix/history")
            drawerLayout.closeDrawers()
        }

        navView.findViewById<TextView>(R.id.nav_problemhistory_reg).setOnClickListener {
            updateUrl("https://m.sunq.co.kr/device-management/error-fix/regist")
            drawerLayout.closeDrawers()
        }
    }

    fun updateUrl(newUrl: String) {
        val currentFragment =
            supportFragmentManager.primaryNavigationFragment?.childFragmentManager?.fragments?.firstOrNull()
        Log.d("MainActivity", "Current fragment: ${currentFragment?.javaClass?.simpleName}")
        if (currentFragment is UrlHandler) {
            currentFragment.updateUrl(newUrl)
        } else {
            Log.e(
                "MainActivity", "Current fragment does not implement UrlHandler: ${currentFragment?.javaClass?.simpleName}"
            )
        }
    }

    fun showUpdateDialog(isAppVersion: Boolean) {
        Log.d("FCM", "showUpdateDialog called with isAppVersion: $isAppVersion")
        if (!isAppVersion) {
            // 커스텀 다이얼로그 레이아웃을 인플레이트
            val dialogView = layoutInflater.inflate(R.layout.custom_update_dialog, null)

            // 다이얼로그 생성 및 설정
            val dialog = Dialog(this)
            dialog.setContentView(dialogView)
            dialog.setCancelable(false)

            // 다이얼로그 배경을 투명하게 설정
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

            // 다이얼로그 애니메이션 설정
            dialog.window?.attributes?.windowAnimations = R.style.DialogAnimation

            // 레이아웃의 뷰 요소 찾기
            val updateButton = dialogView.findViewById<AppCompatButton>(R.id.update_button)
            val cancelButton = dialogView.findViewById<AppCompatButton>(R.id.cancel_button)

            // 업데이트 버튼 클릭 리스너 설정
            updateButton.setOnClickListener {
                val appPackageName = this.packageName
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$appPackageName")))
                } catch (e: android.content.ActivityNotFoundException) {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$appPackageName")))
                }
                dialog.dismiss()
            }

            // 취소 버튼 클릭 리스너 설정
            cancelButton.setOnClickListener {
                dialog.dismiss()
            }

            // 다이얼로그 표시
            dialog.show()
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Log.d("MainActivity", "onNewIntent navigateTo: ${intent?.getStringExtra("navigateTo")}")
        intent?.let {
            handleIntent(it)
        }
    }

    private fun handleIntent(intent: Intent?) {
        intent?.getStringExtra("navigateTo")?.let { navigateTo ->
            Log.d("MainActivity", "handleIntent navigateTo: $navigateTo")
            if (navigateTo == "AlarmFragment") {
                val navController = findNavController(R.id.nav_host_fragment_content_main)
                navController.navigate(R.id.nav_alarmList)
            }
        }
    }

    override fun onBackPressed() {
        val currentFragment =
            supportFragmentManager.primaryNavigationFragment?.childFragmentManager?.fragments?.firstOrNull()
        if (currentFragment is HomeFragment) {
            if (currentFragment.canGoBack()) {
                currentFragment.goBack()
            } else {
                super.onBackPressed()
            }
        } else {
            super.onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return when (item.itemId) {
            R.id.action_settings -> {
                navController.navigate(R.id.nav_setting)
                true
            }
            R.id.action_AlarmList -> {
                navController.navigate(R.id.nav_alarmList)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    private fun collapseAllExcept(exceptListView: ExpandableListView, groupPosition: Int) {
        val listViews = listOf(expandableListViewPlantList)
        for (listView in listViews) {
            if (listView != exceptListView) {
                val adapter = listView.expandableListAdapter
                val groupCount = adapter.groupCount
                for (i in 0 until groupCount) {
                    if (listView.isGroupExpanded(i)) {
                        listView.collapseGroup(i)
                    }
                }
            }
        }
    }

    private fun setExpandableListViewHeight(
        listView: ExpandableListView,
        adapter: ExpandableListAdapter
    ) {
        val listAdapter = listView.expandableListAdapter ?: return

        var totalHeight = 0
        for (i in 0 until adapter.groupCount) {
            val groupItem = adapter.getGroupView(i, false, null, listView)
            groupItem.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
            totalHeight += groupItem.measuredHeight

            if (listView.isGroupExpanded(i)) {
                for (j in 0 until adapter.getChildrenCount(i)) {
                    val listItem = adapter.getChildView(i, j, false, null, listView)
                    listItem.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
                    totalHeight += listItem.measuredHeight

                    listItem.layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                }
            }
        }

        val params = listView.layoutParams
        val oldHeight = params.height
        params.height = totalHeight + (listView.dividerHeight * (adapter.groupCount - 1))
        listView.layoutParams = params
        listView.requestLayout()

        val animator = ValueAnimator.ofInt(oldHeight, params.height)
        animator.addUpdateListener { animation ->
            listView.layoutParams.height = animation.animatedValue as Int
            listView.requestLayout()
        }
        animator.duration = 250
        animator.start()
    }

    fun prepareListData() {
        // Clear existing data
        listDataHeaderPlantList.clear()
        listDataChildPlantList.clear()

        // Add header
        listDataHeaderPlantList.add("발전소 리스트")
        val plantList: MutableList<String> = ArrayList()

        // TokenManager에서 발전소 리스트 가져오기
        val tokenManager = TokenManager(this)
        val savedPlantList = tokenManager.getPlantList()

        savedPlantList?.forEach { plant ->
            plantList.add(plant.plant_name)
        }

        listDataChildPlantList[listDataHeaderPlantList[0]] = plantList

        // 어댑터에 데이터 설정 후 UI 업데이트
        listAdapterPlantList = ExpandableListAdapter(this, listDataHeaderPlantList, listDataChildPlantList)
        expandableListViewPlantList.setAdapter(listAdapterPlantList)
        listAdapterPlantList.notifyDataSetChanged()
    }

    private fun setupExpandableListViews() {
        prepareListData()

        val generationListAdapter =
            ExpandableListAdapter(this, listDataHeaderPlantList, listDataChildPlantList)
        val generationExpandableListView =
            findViewById<ExpandableListView>(R.id.plant_name_list)
        generationExpandableListView.setAdapter(generationListAdapter)
    }
}