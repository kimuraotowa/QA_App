package jp.techacademy.otowa.kimura.qa_app

import android.content.Intent
import android.os.Bundle
import android.util.Base64
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.view.GravityCompat
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import jp.techacademy.otowa.kimura.qa_app.databinding.ActivityMainBinding
class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    //ViewBindingの定義
    private lateinit var binding: ActivityMainBinding

    private var genre = 0

    //databaseReference：FirebaseのRealtime Databaseの参照を保存するための変数
    private lateinit var databaseReference: DatabaseReference

    //questionArrayList：質問データを格納するためのリスト
    private lateinit var questionArrayList: ArrayList<Question>

    //adapter:質問データを表示するためのアダプター
    private lateinit var adapter: QuestionsListAdapter

    //genreRef：特定のジャンルに対するデータベースの参照を保存するための変数
    private var genreRef: DatabaseReference? = null


    //ChildEventListener:Firebaseからデータを取得する必要があります。データに追加・変化があった時に受け取る
    //データが追加されたときの処理
    private val eventListener = object : ChildEventListener {
        //onChildAdded：質問が追加された時に呼び出されるメソッド
        //データがある場合のみ
        override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
            //dataSnapshot：データの取得し、Mapとして扱う
            val map = dataSnapshot.value as Map<*, *>
            //
            val title = map["title"] as? String ?: ""
            val body = map["body"] as? String ?: ""
            val name = map["name"] as? String ?: ""
            val uid = map["uid"] as? String ?: ""
            val imageString = map["image"] as? String ?: ""

            //画像データのでコード
            //データが存在する場合は、Base64形式からでコードしてbyte配列に変換します。
            //存在しない場合は空のbyte配列を設定します。
            val bytes =
                if (imageString.isNotEmpty()) {
                    Base64.decode(imageString, Base64.DEFAULT)
                } else {
                    byteArrayOf()
                }


            val answerArrayList = ArrayList<Answer>()
            val answerMap = map["answers"] as Map<*, *>?
            if (answerMap != null) {
                for (key in answerMap.keys) {
                    val map1 = answerMap[key] as Map<*, *>
                    val map1Body = map1["body"] as? String ?: ""
                    val map1Name = map1["name"] as? String ?: ""
                    val map1Uid = map1["uid"] as? String ?: ""
                    val map1AnswerUid = key as? String ?: ""
                    val answer = Answer(map1Body, map1Name, map1Uid, map1AnswerUid)
                    //回答のデータをリストに追加
                    answerArrayList.add(answer)
                }
            }
            //質問データのリストへの追加と表示
            val question = Question(
                title, body, name, uid, dataSnapshot.key ?: "",
                genre, bytes, answerArrayList
            )
            questionArrayList.add(question)
            //質問が更新されたことをアダプターに通知してリストViewを更新する
            adapter.notifyDataSetChanged()
        }


        //onChildChanged:質問に対して回答が投稿され
        //
        override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {
            val map = dataSnapshot.value as Map<*, *>

            // 変更があったQuestionを探す
            for (question in questionArrayList) {
                if (dataSnapshot.key.equals(question.questionUid)) {
                    // このアプリで変更がある可能性があるのは回答（Answer)のみ
                    question.answers.clear()
                    val answerMap = map["answers"] as Map<*, *>?
                    if (answerMap != null) {
                        for (key in answerMap.keys) {
                            val map1 = answerMap[key] as Map<*, *>
                            val map1Body = map1["body"] as? String ?: ""
                            val map1Name = map1["name"] as? String ?: ""
                            val map1Uid = map1["uid"] as? String ?: ""
                            val map1AnswerUid = key as? String ?: ""
                            val answer = Answer(map1Body, map1Name, map1Uid, map1AnswerUid)
                            question.answers.add(answer)
                        }
                    }

                    adapter.notifyDataSetChanged()
                }
            }
        }

        //絶対に書かないといけない
        override fun onChildRemoved(p0: DataSnapshot) {}
        override fun onChildMoved(p0: DataSnapshot, p1: String?) {}
        override fun onCancelled(p0: DatabaseError) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //ツールバーの設定
        setSupportActionBar(binding.content.toolbar)

        //フローティングアクションボタン(画面右下丸いボタン)がクリックされたときのリスナー
        binding.content.fab.setOnClickListener {
            //ジャンルを選択していない場合はメッセージを表示するだけ
            if (genre == 0) {
                Snackbar.make(
                    it,
                    getString(R.string.question_no_select_genre),
                    Snackbar.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }
            // ログイン済みのユーザーを取得する
            val user = FirebaseAuth.getInstance().currentUser

            // ログインしていなければログイン画面に遷移させる
            if (user == null) {
                //ログインしてない場合LoginActivityに戻る
                val intent = Intent(applicationContext, LoginActivity::class.java)
                startActivity(intent)
            } else {
                //ジャンルを選択して質問作成画面を起動する
                val intent = Intent(applicationContext, QuestionSendActivity::class.java)
                intent.putExtra("genre", genre)
                startActivity(intent)
            }
        }

        // ナビゲーションドロワーの設定
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
        binding.navView.setNavigationItemSelectedListener(this)

        // Firebaseを作るため
        databaseReference = FirebaseDatabase.getInstance().reference

        // ListViewの準備
        adapter = QuestionsListAdapter(this)
        questionArrayList = ArrayList()
        adapter.notifyDataSetChanged()

        binding.content.inner.listView.setOnItemClickListener { _, _, position, _ ->
            //質問の詳細画面を起動する
            //QuestionDetailActivityを起動
            val intent = Intent(applicationContext, QuestionDetailActivity::class.java)
            //質問データ渡す
            intent.putExtra("question", questionArrayList[position])
            //起動
            startActivity(intent)
        }

        updateNavigationView()

    }

    //Activity再会時に呼ばれる（更新する処理）
    override fun onResume() {
        super.onResume()
        //ナビゲーションView取得
        val navigationView = findViewById<NavigationView>(R.id.nav_view)

        if (genre == 0) {
            //ナビゲーションメニューの最初のアイテムを選択。
            onNavigationItemSelected(navigationView.menu.getItem(0))
        }
        updateNavigationView()
    }

    //オプションメニュー
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    //ツールバーのメニューアイテムが選択された時の処理
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        //選択されたアイテムのID取得
        val id = item.itemId

        //設定画面を起動
        if (id == R.id.action_settings) {
            //SettingActivityに移動
            val intent = Intent(applicationContext, SettingActivity::class.java)
            //起動
            startActivity(intent)
            //メニューアイテムの選択が終了
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    //メニューアイテムが選択された時の処理
    //メニューアイテム選択：ツールバーのタイトル変更、ジャンルを表すgenre変数値の
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
            R.id.nav_favorite -> {
                binding.content.toolbar.title = getString(R.string.menu_favorite_label)
            }
        }

        //ドロワーを閉じる
        //GravityCompat.START:左から開くことを示してる。
        binding.drawerLayout.closeDrawer(GravityCompat.START)

        // 質問のリストをクリアしてから再度Adapterにセットし、AdapterをListViewにセットし直す
        questionArrayList.clear()
        //アダプターにに空の質問リストをセットする。
        adapter.setQuestionArrayList(questionArrayList)
        //アダプターとリストViewに再設定する。
        binding.content.inner.listView.adapter = adapter

        // 選択したジャンルにリスナーを登録する
        //すでにジャンルが存在する場合そのリスナーを削除
        if (genreRef != null) {
            genreRef!!.removeEventListener(eventListener)
        }
        //新しいジャンルの設定
        genreRef = databaseReference.child(ContentsPATH).child(genre.toString())
        //新しいジャンルに対してリスナーを登録する
        genreRef!!.addChildEventListener(eventListener)

        //メニューアイテムの選択処理が完了したことを示す
        return true
    }

    //お気に入りの非表示、表示の設定
    private fun updateNavigationView() {
        //ナビゲーションを取得
        val navigationView = findViewById<NavigationView>(R.id.nav_view)
        //ナビゲーションのメニュー取得
        val menu = navigationView.menu
        //現在のユーザー取得
        val user = FirebaseAuth.getInstance().currentUser

        //お気に入りメニュー項目を取得し、ユーザーがログインしている場合のみ表示
        if (user != null) {
            menu.findItem(R.id.nav_favorite).isVisible = true
        } else {
            menu.findItem(R.id.nav_favorite).isVisible = false
        }
    }
}
