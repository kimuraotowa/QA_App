package jp.techacademy.otowa.kimura.qa_app

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.preference.PreferenceManager
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import jp.techacademy.otowa.kimura.qa_app.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding

    //Firebase:認証機能を提供するクラス(ログインやアカウントの作成)
    private lateinit var auth: FirebaseAuth

    //アカウト作成の結果を受け取るリスナー
    //AuthResult:認証の結果を表すクラス
    private lateinit var createAccountListener: OnCompleteListener<AuthResult>

    //ログイン結果を受け取るリスナー
    private lateinit var loginListener: OnCompleteListener<AuthResult>

    //DatabaseReference:リアルタイムDatabaseの特定の位置を表す。
    //データの読み書きやリスナーの設定を行う
    private lateinit var databaseReference: DatabaseReference

    // アカウント作成時にフラグを立て、ログイン処理後に名前をFirebaseに保存する
    private var isCreateAccount = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //データベースのリファレンス取得
        databaseReference = FirebaseDatabase.getInstance().reference

        //FirebaseAuthのオブジェクトを取得する
        //↓Firebaseの認証機能が使えるようになる
        auth = FirebaseAuth.getInstance()

        //アカウント作成処理のリスナー
        //アカウント作成が完了した時に呼ばれるリスナーの設定
        createAccountListener = OnCompleteListener { task ->
            if (task.isSuccessful) {
                //成功した場合ログインを行う
                //ユーザーが入力したメールアドレスの取得
                val email = binding.emailText.text.toString()

                //ユーザーが入力したパスワードの取得
                val password = binding.passwordText.text.toString()

                //ログイン
                login(email, password)
            } else {
                //アカウントの作成が失敗した場合
                val view = findViewById<View>(android.R.id.content)
                //エラーメッセージの表示
                //ここから
                Snackbar.make(
                    view,
                    getString(R.string.create_account_failure_message),
                    Snackbar.LENGTH_LONG
                ).show()
                //ここまで

                //プログレスバーを非表示にする
                //プログレスバー：ログインの棒みたいなの
                binding.progressBar.visibility = View.GONE
            }
        }

        //ログイン処理のリスナー
        //ログイン完了時に呼ばれるリスナー設定
        loginListener = OnCompleteListener { task ->
            //ログインが成功したか判定する
            if (task.isSuccessful) {
                //成功した場合
                //現在ログインしているユーザーの情報を取得してる
                val user = auth.currentUser
                //？？：FirebaseのRealtime Database内の特定の位置
                // （ここでは、ログインユーザーのIDに対応する位置）を参照するDatabaseReferenceを取得
                val userRef = databaseReference.child(UsersPATH).child(user!!.uid)

                //アカウント作成時かどうかを判定
                if (isCreateAccount) {
                    // アカウント作成の時は表示名をFirebaseに保存する
                    //ユーザーが入力した名前の取得
                    val name = binding.nameText.text.toString()

                    //データを保存するためのHashMap作成
                    val data = HashMap<String, String>()
                    //取得した名前をHashMapに保存
                    data["name"] = name
                    //HashMapのデータをFirebaseのRealtimeDatabaseに保存
                    userRef.setValue(data)

                    //取得した名前をPreference(アプリケーションの設定情報を保存する)に保存する
                    saveName(name)
                } else {
                    //作成できない場合
                    //FirebaseのRealtime Databaseから一度だけデータを読み取るためのリスナーを設定するコードです。
                    // addListenerForSingleValueEvent:一度だけデータを読み取る
                    //addValueEventListener:データが更新されるたびに読み取る
                    userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                        //FirebaseのRealtime Databaseから名前を取得し、Preferenceに保存します
                        //useRefのデータを一度だけ読み取り、
                        // その結果をValueEventListenerのonDataChangeでうけとる
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val data = snapshot.value as Map<*, *>?
                            //読み取ったデータ(ユーザーの名前を取り出す) → String型に変換 → Preferenceに保存
                            saveName(data!!["name"] as String)
                        }

                        //データからエラーが発生した時に呼ばれる
                        //中身が空　→　何も行わない
                        override fun onCancelled(firebaseError: DatabaseError) {}
                    })
                }

                // プログレスバーを非表示にする
                binding.progressBar.visibility = View.GONE

                // Activityを閉じる
                finish()

            } else {
                // 失敗した場合
                // エラーを表示する
                val view = findViewById<View>(android.R.id.content)
                Snackbar.make(view, getString(R.string.login_failure_message), Snackbar.LENGTH_LONG)
                    .show()

                // プログレスバーを非表示にする
                binding.progressBar.visibility = View.GONE
            }
        }

        // タイトルの設定
        //login_titleの文字列取得
        title = getString(R.string.login_title)

        //アカウント作成ボタンがクリックされたときのリスナーを設定している
        binding.createButton.setOnClickListener { v ->
            //クリック→InputMethodManagerのhideSoftInputFromWindow呼び出し→キーボード非表示
            //キーボードを管理するInputMethodManagerを取得
            val im = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            //キーボードがでたら閉じる
            im.hideSoftInputFromWindow(v.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)

            //ユーザーが入力したPW,名前、メアド取得
            val email = binding.emailText.text.toString()
            val password = binding.passwordText.text.toString()
            val name = binding.nameText.text.toString()

            //名前が空でなく、PWが6文字以上であるか確認
            if (email.isNotEmpty() && password.length >= 6 && name.isNotEmpty()) {
                // ログイン時に表示名を保存するようにフラグを立てる
                isCreateAccount = true
                //メアドとPWを使用してアカウント作成処理をする
                createAccount(email, password)
            } else {
                 //条件を満たさない場合
                //エラーメッセージの表示
                Snackbar.make(v, getString(R.string.login_error_message), Snackbar.LENGTH_LONG)
                    .show()
            }
        }
        //ユーザーがログインボタンをクリックした時の処理
        binding.loginButton.setOnClickListener { v ->
            //キーボードを管理するInputMethodManagerを取得
            val im = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            //キーボードの非表示
            im.hideSoftInputFromWindow(v.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
            //メアドとPWを取得
            val email = binding.emailText.text.toString()
            val password = binding.passwordText.text.toString()

            //メアドが空じゃない、PWが6文字以上であるか判断
            if (email.isNotEmpty() && password.length >= 6) {
                // フラグを落としておく
                isCreateAccount = false

                //メアドとPWを使用して処理
                login(email, password)
            } else {
                //メールアドレスやパスワードが条件を満たさない場合は、エラーメッセージを表示
                // エラーを表示する
                Snackbar.make(v, getString(R.string.login_error_message), Snackbar.LENGTH_LONG)
                    .show()
            }
        }
    }

    //アカウント作成を行う関数を定義してる
    //この関数はメールアドレスとパスワードを引数に取り、
    private fun createAccount(email: String, password: String) {
        // プログレスバーを表示する
        binding.progressBar.visibility = View.VISIBLE

        // アカウントを作成する
        // FirebaseのcreateUserWithEmailAndPasswordメソッドを使用してアカウントを作成します。
        auth.createUserWithEmailAndPassword(email, password)
            // アカウント作成が完了したら、
            // addOnCompleteListener(createAccountListener)で設定したリスナーが呼ばれます
            .addOnCompleteListener(createAccountListener)
    }

    //ログインを行う関数を定義してる
    private fun login(email: String, password: String) {
        // プログレスバーを表示する
        binding.progressBar.visibility = View.VISIBLE

        // ログインする
        //FirebaseのsignInWithEmailAndPasswordメソッドを使用してログイン
        auth.signInWithEmailAndPassword(email, password)
            //addOnCompleteListener(loginListener)で設定したリスナーが呼ぶ
            .addOnCompleteListener(loginListener)
    }

    //名前を保存する関数を定義してる。
    private fun saveName(name: String) {
        // Preferenceに保存する
        val sp = PreferenceManager.getDefaultSharedPreferences(this)
        val editor = sp.edit()
        editor.putString(NameKEY, name)
        editor.apply()
        //SharedPreferencesはアプリケーションの設定情報を保存するためのもので
        // editor.putString(NameKEY, name)で名前を保存し、editor.apply()で変更を適用しています
    }
}