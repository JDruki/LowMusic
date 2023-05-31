package com.wangjingbo.low.Activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.wangjingbo.low.R

class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        val bottomNavView = findViewById<BottomNavigationView>(R.id.bottomNavView)
        bottomNavView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.menu_search -> {
                    navController.navigate(R.id.search)
                    true
                }
                R.id.menu_music_list -> {
                    navController.navigate(R.id.musicList)
                    true
                }
                R.id.menu_playlist -> {
                    navController.navigate(R.id.playlist)
                    true
                }
                R.id.menu_heartmusic -> {
                    navController.navigate(R.id.heartmusic)
                    true
                }
                else -> false
            }
        }
    }
}