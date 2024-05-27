package jp.techacademy.otowa.kimura.qa_app

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.view.GravityCompat
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import jp.techacademy.otowa.kimura.qa_app.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(),NavigationView.OnNavigationItemSelectedListener {
    //ViewBindingの定義
    private lateinit var binding: ActivityMainBinding

    private var genre = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityMainBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        //ツールバーの設定
        setSupportActionBar(binding.content.toolbar)

        //フローティングアクションボタン(画面右下丸いのボタン)がクリックされたときのリスナー
        binding.content.fab.setOnClickListener {

            //ログイン済みのユーザーを取得
            val user = FirebaseAuth.getInstance().currentUser

            //ログインしていなければログイン画面に戻る
            if (user == null) {
                //リグインしてない場合LoginActivityに戻る
                val intent = Intent(applicationContext, LoginActivity::class.java)
                startActivity(intent)
            }
        }

        //ドロワー(スライドメニュー)の設定
        //ActionBarDrawerToggle：ナビゲーションドロワーとアクションバーを連携させるためのクラス
        val toggle = ActionBarDrawerToggle(
            this,
            //ドロワーレイアウト
            binding.drawerLayout,
            //ツールバー
            binding.content.toolbar,
            //ドロワーが開いた時、閉じた時の説明
            R.string.app_name,
            R.string.app_name
        )
        //ドロワーが開いたり閉じたりした時のイベント処理
        binding.drawerLayout.addDrawerListener(toggle)
        //アクションバー上のドロワーの状態同期
        toggle.syncState()

        //ドロワーのメニューが選択された時の処理
        //this:MainActivity
        binding.navView.setNavigationItemSelectedListener(this)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    //ツールバーのメニューアイテムが選択された時の処理
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        //item.itemId：で選択されたメニューアイテムのIDを取得
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    //メニューアイテムが選択された時の処理
    //メニューアイテム選択：ツールバーのタイトル変更、ジャンルを表すgenre変数値の更新。
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_hobby -> {
                binding.content.toolbar.title = getString(R.string.menu_hobby_label)
                genre = 1
            }
            R.id.nav_life -> {
                binding.content.toolbar.title = getString(R.string.menu_life_label)
                genre = 2
            }
            R.id.nav_health -> {
                binding.content.toolbar.title = getString(R.string.menu_health_label)
                genre = 3
            }
            R.id.nav_computer -> {
                binding.content.toolbar.title = getString(R.string.menu_computer_label)
                genre = 4
            }
        }
        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }
}