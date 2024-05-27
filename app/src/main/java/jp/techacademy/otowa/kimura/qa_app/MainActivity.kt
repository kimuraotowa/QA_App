package jp.techacademy.otowa.kimura.qa_app

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import com.google.firebase.auth.FirebaseAuth
import jp.techacademy.otowa.kimura.qa_app.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    //ViewBindingの定義
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityMainBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        //ツールバーの設定
        setSupportActionBar(binding.toolbar)

        //フローティングアクションボタン(画面右下丸いのボタン)がクリックされたときのリスナー
        binding.fab.setOnClickListener{

        //ログイン済みのユーザーを取得
        val user = FirebaseAuth.getInstance().currentUser

        //ログインしていなければログイン画面に戻る
        if (user == null) {
            //リグインしてない場合LoginActivityに戻る
            val intent = Intent(applicationContext, LoginActivity::class.java)
            startActivity(intent)
        }
    }
}

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }
}