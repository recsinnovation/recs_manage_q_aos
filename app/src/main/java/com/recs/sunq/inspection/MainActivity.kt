package com.recs.sunq.inspection

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.navigation.NavigationView
import com.recs.sunq.inspection.databinding.ActivityMainBinding
import com.recs.sunq.inspection.ui.home.HomeFragment

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarMain.toolbar)

        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        setupNavigationListeners(navView, drawerLayout)
    }

    private fun setupNavigationListeners(
        navView: NavigationView,
        drawerLayout: DrawerLayout
    ) {
        navView.findViewById<TextView>(R.id.nav_main_screen).setOnClickListener {
            updateUrl("https://192.168.0.28:5173/device-management/inspection/history")
            drawerLayout.closeDrawers()
        }

        navView.findViewById<TextView>(R.id.nav_power_system).setOnClickListener {
            updateUrl("https://192.168.0.28:5173/device-management/inspection/regist")
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
                "MainActivity",
                "Current fragment does not implement UrlHandler: ${currentFragment?.javaClass?.simpleName}"
            )
        }
    }

    fun showUpdateDialog(isAppVersion: Boolean) {
        Log.d("FCM", "showUpdateDialog called with isAppVersion: $isAppVersion")
        if (!isAppVersion) {
            val builder = AlertDialog.Builder(this) // Activity의 Context 전달
            builder.setTitle("업데이트 필요")
            builder.setMessage("앱 버전이 최신이 아닙니다. 업데이트가 필요합니다.")
            builder.setPositiveButton("업데이트") { _, _ ->
                val appPackageName = this.packageName
                try {
                    startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("market://details?id=$appPackageName")
                        )
                    )
                } catch (e: android.content.ActivityNotFoundException) {
                    startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://play.google.com/store/apps/details?id=$appPackageName")
                        )
                    )
                }
            }
            builder.setNegativeButton("취소") { dialog, _ ->
                dialog.dismiss()
            }
            builder.show()
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
}