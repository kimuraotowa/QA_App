package jp.techacademy.otowa.kimura.qa_app


import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import jp.techacademy.otowa.kimura.qa_app.databinding.ActivityQuestionDetailBinding

//質問の詳細と回答を表示する
class QuestionDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityQuestionDetailBinding

    //質問を格納するための変数
    private lateinit var question: Question

    //質問の詳細と回答を表示するためのアダプター
    private lateinit var adapter: QuestionDetailListAdapter

    //Firebaseの回答データを参照を保存するための変数。
    private lateinit var answerRef: DatabaseReference

    //Firebaseにお気に入りデータを保存するための変数
    private lateinit var favoriteRef: DatabaseReference

    //星がタップされたかどうかの変数
    private var isFavorite = false

    private val eventListener = object : ChildEventListener {
        override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
            val map = dataSnapshot.value as Map<*, *>

            //回答のUIDの取得
            val answerUid = dataSnapshot.key ?: ""

            //重複チェック
            //重複した回答がリストに追加されるのを防ぐ。
            for (answer in question.answers) {
                // 同じAnswerUidのものが存在しているときは何もしない
                if (answerUid == answer.answerUid) {
                    return
                }
            }

            val body = map["body"] as? String ?: ""
            val name = map["name"] as? String ?: ""
            val uid = map["uid"] as? String ?: ""

            //Answerを作成し、質問と回答リストに追加します。
            val answer = Answer(body, name, uid, answerUid)
            question.answers.add(answer)
            //アダプターにデータの更新を通知し、リストViewを更新する。
            adapter.notifyDataSetChanged()
        }

        //ChildEventListener:必ず必要なメソッド
        override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {}
        override fun onChildRemoved(dataSnapshot: DataSnapshot) {}
        override fun onChildMoved(dataSnapshot: DataSnapshot, s: String?) {}
        override fun onCancelled(databaseError: DatabaseError) {}
    }

    //アプリの初めて起動した時に呼ばれる
    //レイアウトの設定　初期化処理を行っている
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQuestionDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 渡ってきたQuestionのオブジェクトを保持する
        // API33以上でgetSerializableExtra(key)が非推奨となったため処理を分岐
        @Suppress("UNCHECKED_CAST", "DEPRECATION", "DEPRECATED_SYNTAX_WITH_DEFINITELY_NOT_NULL")
        question = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            intent.getSerializableExtra("question", Question::class.java)!!
        else
            intent.getSerializableExtra("question") as? Question!!

        //取得したQuestionのタイトルをActivityに設定する
        title = question.title

        // ListViewの準備
        //初期化、リストViewの設定
        adapter = QuestionDetailListAdapter(this, question)
        //アダプターにデータの変更の通知、リストView更新
        binding.listView.adapter = adapter
        adapter.notifyDataSetChanged()

        //現在のユーザー取得
        val user = FirebaseAuth.getInstance().currentUser

        //ログインしてる場合ImageView表示
        if (user != null) {
            binding.favoriteImageView.visibility = View.VISIBLE

            //FireBaseのコンテンツ表示
            val dataBaseReference = FirebaseDatabase.getInstance().reference
            favoriteRef =
                dataBaseReference.child(FavoritePATH).child(user.uid).child(question.questionUid)

            //Firebaseからお気に入りのデータを一度だけ取得
            favoriteRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    //isFavorite変数にお気に入りの状態を設定
                    isFavorite = dataSnapshot.exists()
                    updateFavoriteImage()
                }
                //データの取得がキャンセルされた時
                override fun onCancelled(firebaseError: DatabaseError) {}
            })
            binding.favoriteImageView.setOnClickListener {

            }

            // 星のImageViewをクリックしたときの処理
            binding.favoriteImageView.setOnClickListener {
                val user = FirebaseAuth.getInstance().currentUser
                if (user == null) {
                    // ログインしていない場合はログイン画面に遷移
                    val intent = Intent(applicationContext, LoginActivity::class.java)
                    startActivity(intent)

                } else {
                    //反転の処理
                    isFavorite = !isFavorite
                    updateFavoriteImage()

                    //Firebaseの保存
                    if (isFavorite) {
                        val favoriteData = mapOf("genre" to question.genre.toString())
                        favoriteRef.setValue(favoriteData)
                    } else {
                        //削除
                        favoriteRef.removeValue()
                    }
                }
            }

            //Fabをクリックしたときの処理
            binding.fab.setOnClickListener {
                // ログイン済みのユーザーを取得する
                val user = FirebaseAuth.getInstance().currentUser

                if (user == null) {
                    // ログインしていなければログイン画面(LoginActivity)に遷移させる
                    val intent = Intent(applicationContext, LoginActivity::class.java)
                    startActivity(intent)
                } else {
                    // Questionを渡して回答作成画面を起動する
                    val intent = Intent(applicationContext, AnswerSendActivity::class.java)
                    intent.putExtra("question", question)
                    startActivity(intent)
                }
            }

            //質問のUIDに基づいて、回答データの参照を設定
            answerRef = dataBaseReference.child(ContentsPATH).child(question.genre.toString())
                .child(question.questionUid).child(AnswersPATH)

            //回答データの変更を監視するリスナーを設定
            answerRef.addChildEventListener(eventListener)

        } else {
            binding.favoriteImageView.visibility = View.GONE
        }
    }

    // 星の画像を更新する
    private fun updateFavoriteImage() {
        if (isFavorite) {
            binding.favoriteImageView.setImageResource(R.drawable.ic_star)
        } else {
            binding.favoriteImageView.setImageResource(R.drawable.ic_star_border)
        }
    }
}
