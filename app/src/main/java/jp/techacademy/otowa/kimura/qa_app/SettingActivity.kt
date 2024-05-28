package jp.techacademy.otowa.kimura.qa_app


import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import androidx.preference.PreferenceManager
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import jp.techacademy.otowa.kimura.qa_app.databinding.ActivitySettingBinding

class SettingActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingBinding

    private lateinit var databaseReference: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Preferenceから表示名を取得してEditTextに反映させる
        //アプリの設定情報を保存しているSharedPreferencesを取得している。
        val sp = PreferenceManager.getDefaultSharedPreferences(this)
        //NameKEYで保存されている表示名を取り出してる
        val name = sp.getString(NameKEY, "")
        //表示名をTextBoxに表示している
        binding.nameText.setText(name)

        //Firebaseのデータベースを取得しdatabaseReference に保存
        databaseReference = FirebaseDatabase.getInstance().reference

        // UIの初期設定
        //画面の上のタイトルの設定
        // R.stringは文字列でsetting_titleに保存されている
        title = getString(R.string.settings_title)

        //表示名返納ボタンが押された時の処理
        binding.changeButton.setOnClickListener { v ->
            //キーボードを操作するためのツール
            val im = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            // キーボードが出ていたら閉じる
            im.hideSoftInputFromWindow(v.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)

            // ログイン済みのユーザーを取得する
            //ユーザーがいればuserに保存される
            val user = FirebaseAuth.getInstance().currentUser

            //ログインしているユーザーがいなければ以下を使ってメッセージの表示
            if (user == null) {
                // ログインしていない場合は何もしない
                Snackbar.make(v, getString(R.string.no_login_user), Snackbar.LENGTH_LONG).show()
            } else {
                // 変更した表示名をFirebaseに保存する
                val name2 = binding.nameText.text.toString()
                //保存する場所を決める
                val userRef = databaseReference.child(UsersPATH).child(user.uid)
                val data = HashMap<String, String>()
                data["name"] = name2
                userRef.setValue(data)

                // 変更した表示名をPreferenceに保存する
                //保存する場所の取得
                val sp2 = PreferenceManager.getDefaultSharedPreferences(applicationContext)
                val editor = sp2.edit()
                //新しい名前を保存
                editor.putString(NameKEY, name2)
                editor.apply()

                //メッセージの表示
                Snackbar.make(v, getString(R.string.change_display_name), Snackbar.LENGTH_LONG)
                    .show()
            }
        }

        //ログアウトボタンが押された時の処理
        binding.logoutButton.setOnClickListener { v ->
            //ログアウトする・アプリからユーザーの登録情報が削除
            FirebaseAuth.getInstance().signOut()
            //テキストボックスの内容を削除
            binding.nameText.setText("")
            //メッセージ表示：ログアウトが完了しました。
            Snackbar.make(v, getString(R.string.logout_complete_message), Snackbar.LENGTH_LONG)
                .show()
        }
    }
}