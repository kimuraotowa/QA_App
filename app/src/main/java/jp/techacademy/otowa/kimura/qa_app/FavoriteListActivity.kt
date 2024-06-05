package jp.techacademy.otowa.kimura.qa_app

import com.google.firebase.auth.FirebaseAuth
import android.os.Bundle
import android.content.Intent
import android.util.Base64
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.*
import jp.techacademy.otowa.kimura.qa_app.databinding.ActivityFavoriteListBinding

class FavoriteListActivity:AppCompatActivity() {
    private lateinit var binding: ActivityFavoriteListBinding

    //質問を表示するためのアダプター
    private lateinit var adapter: QuestionsListAdapter

    //お気に入りの質問を参照
    private lateinit var favoriteRef: DatabaseReference

    //お気に入りのリストを格納するためのリスト
    private var favoriteArrayList = ArrayList<Question>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFavoriteListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //bindingの初期化。レイアウトを設定
        //adapterの初期化。リストViewに設定
        adapter = QuestionsListAdapter(this)
        binding.listView.adapter = adapter

        //現在のユーザーの取得
        //ログインしている場合お気に入りの質問を参照するリファレンスを設定
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            val dataBaseReference = FirebaseDatabase.getInstance().reference
            favoriteRef = dataBaseReference.child(FavoritePATH).child(user.uid)

            //お気に入りの質問が追加された時のリスナーを設定
            favoriteRef.addChildEventListener(object : ChildEventListener {
                //お気に入りが追加された時に、その質問の詳細を一度だけ取得
                override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
                    //Firebase Realtime Database から取得したデータを適切に処理し、質問のお気に入り情報を取得する
                    //Firebaseのなかのデータを取得。取得したデータをMap型にする
                    val favorite = dataSnapshot.value as Map<*, *>
                    //nullの場合空文字列。質問のUIDを取得
                    val questionUid = dataSnapshot.key ?: ""
                    //favoriteのジャンルから、Key：genreを取得。genreはInt型だから、String型に変更
                    val genre = favorite["genre"].toString()

                    val questionRef =
                        dataBaseReference.child(ContentsPATH).child(genre).child(questionUid)

                    //質問が追加された時のリスナー
                    questionRef.addListenerForSingleValueEvent(object : ValueEventListener {
                        //質問が追加された時に、その質問の詳細を一度だけ取得
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            val map = dataSnapshot.value as Map<*, *>?
                            if (map != null) {
                                val title = map["title"] as? String ?: ""
                                val body = map["body"] as? String ?: ""
                                val name = map["name"] as? String ?: ""
                                val uid = map["uid"] as? String ?: ""
                                val imageString = map["image"] as? String ?: ""
                                val bytes = if (imageString.isNotEmpty()) Base64.decode(
                                    imageString,
                                    Base64.DEFAULT
                                ) else byteArrayOf()

                                //回答データの処理
                                //回答の初期化
                                val answers = ArrayList<Answer>()
                                //質問データの中から回答データを取得
                                val answerMap = map["answers"] as Map<*, *>?
                                //answerMapがNullでない場合、回答データが存在することを意味する
                                if (answerMap != null) {
                                    for (key in answerMap.keys) {
                                        val temp = answerMap[key] as Map<*, *>
                                        //回答オブジェクトの作成
                                        val answerBody = temp["body"] as? String ?: ""
                                        val answerName = temp["name"] as? String ?: ""
                                        val answerUid = temp["uid"] as? String ?: ""
                                        //回答のオブジェクト作成
                                        val answer =
                                            Answer(answerBody, answerName, answerUid, key as String)
                                        //回答リストへの追加
                                        answers.add(answer)
                                    }
                                }

                                //質問オブジェクトの作成
                                val question = Question(title, body, name, uid, questionUid, genre.toInt(), bytes, answers)

                                //お気に入りリストへの追加
                                favoriteArrayList.add(question)
                                //新しい質問リストを設定
                                adapter.setQuestionArrayList(favoriteArrayList)
                                //アダプターへの設定と変更通知
                                adapter.notifyDataSetChanged()
                            }
                        }

                        override fun onCancelled(databaseError: DatabaseError) {}
                    })
                }

                //質問の内容を変える時に呼ばれる
                override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {}

                //onChildRemoved:お気に入りリストから質問が削除された場合に、その質問をリストから削除し、UIを更新するために使用される
                override fun onChildRemoved(dataSnapshot: DataSnapshot) {
                    //削除された質問のUIDを取得
                    val questionUid = dataSnapshot.key ?: ""
                    //削除された質問をリストから削除
                    favoriteArrayList.removeAll { it.questionUid == questionUid }
                    //アダプターに新しい質問を設定し、データの更新を通知
                    adapter.setQuestionArrayList(favoriteArrayList)
                    adapter.notifyDataSetChanged()
                }

                //リスト内のアイテムの順序が変更された場合に、その順序を反映してUIを更新するために使用する
                override fun onChildMoved(dataSnapshot: DataSnapshot, s: String?) {}
                //順序が変更された場合に、その変更を反映するために使用する。
                override fun onCancelled(databaseError: DatabaseError) {}
            })

            binding.listView.setOnItemClickListener { _, _, position, _ ->
                //質問の詳細画面を起動する
                //QuestionDetailActivityを起動
                val intent = Intent(applicationContext, QuestionDetailActivity::class.java)
                //質問データ渡す
                intent.putExtra("question", favoriteArrayList[position])
                //起動
                startActivity(intent)
            }

        } else {
            //ログインしていない場合LoginActivityに遷移
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}

