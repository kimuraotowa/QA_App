package jp.techacademy.otowa.kimura.qa_app

import android.content.Context
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.preference.PreferenceManager
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import jp.techacademy.otowa.kimura.qa_app.databinding.ActivityAnswerSendBinding


//特定の質問に回答を送信するためのActivity
//View.OnClickListener：ボタンをクリックした時のイベント処理するためのインターフェース
//DatabaseReference.CompletionListener:Firebaseデータベース操作の完了を処理するためのインターフェイス
class AnswerSendActivity : AppCompatActivity(), View.OnClickListener,
DatabaseReference.CompletionListener{
    private lateinit var binding: ActivityAnswerSendBinding

    private lateinit var question: Question

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAnswerSendBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 渡ってきたQuestionのオブジェクトを保持する
        // API33以上でgetSerializableExtra(key)が非推奨となったため処理を分岐
        @Suppress("UNCHECKED_CAST", "DEPRECATION", "DEPRECATED_SYNTAX_WITH_DEFINITELY_NOT_NULL")
        question = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            intent.getSerializableExtra("question", Question::class.java)!!
        else
            intent.getSerializableExtra("question") as? Question!!

        // UIの準備
        //View.onClickListenerを実装しているため、OnClickメソッドが呼ばれる
        binding.sendButton.setOnClickListener(this)
    }

    //Firebaseデータベース操作が完了した時に呼ばれる
    override fun onComplete(databaseError: DatabaseError?, databaseReference: DatabaseReference) {
        //データベース操作が完了したら、プロブレスバーを非表示
        binding.progressBar.visibility = View.GONE

        //操作成功時の処理
        //databaseErrorがnullであれば成功したと示す。その場合Activity終了
        if (databaseError == null) {
            finish()
        } else {
            //操作失敗の処理
            Snackbar.make(
                findViewById(android.R.id.content),
                getString(R.string.send_answer_failure),
                //スナックバーを使用して(send_answer_failure)投稿に失敗しました表示
                Snackbar.LENGTH_LONG
            ).show()
        }
    }

    //ユーザーが回答を入力して送信ボタンをクリックしたときの処理
    override fun onClick(v: View) {
        // キーボードが出てたら閉じる
        val im = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        im.hideSoftInputFromWindow(v.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)

        val dataBaseReference = FirebaseDatabase.getInstance().reference
        //質問とジャンルとUIDに基づいて、回答データの参照を設定する
        val answerRef = dataBaseReference.child(ContentsPATH).child(question.genre.toString())
            .child(question.questionUid).child(AnswersPATH)

        //データを格納するためのハッシュマップ作成
        val data = HashMap<String, String>()

        // UID
        //現在ログインしているユーザーのUIDを取得、データに追加
        data["uid"] = FirebaseAuth.getInstance().currentUser!!.uid

        // 表示名
        // Preferenceから名前を取る
        val sp = PreferenceManager.getDefaultSharedPreferences(this)
        val name = sp.getString(NameKEY, "")
        //取得した、表示名をデータに追加します。
        data["name"] = name!!

        // 回答を取得する
        val answer = binding.answerEditText.text.toString()

        //回答が入力されていない場合、エラーメッセージを表示して、処理を終了する
        if (answer.isEmpty()) {
            // 回答が入力されていない時はエラーを表示するだけ
            Snackbar.make(v, getString(R.string.answer_error_message), Snackbar.LENGTH_LONG).show()
            return
        }
        //answerで取得した回答をデータに追加
        data["body"] = answer

        //回答の送信
        //プログレスバー表示
        binding.progressBar.visibility = View.VISIBLE
        //Firebaseにデータ送信
        //this:DatabaseReference.CompletionListenerを指す。送信が完了したときにonCompleteメソッドが呼ばれる
        answerRef.push().setValue(data, this)
    }
}