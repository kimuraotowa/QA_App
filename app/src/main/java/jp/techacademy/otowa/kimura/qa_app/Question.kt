package jp.techacademy.otowa.kimura.qa_app

import java.io.Serializable
import java.util.ArrayList

class Question(
    val title: String, //Firebaseから取得したタイトル
    val body: String, //Firebaseから取得した質問本文
    val name: String,  //Firebaseから取得した質問者の名前
    val uid: String,  //Firebaseから取得した質問者のUID
    val questionUid: String, //Firebaseから取得した質問のUID
    val genre: Int, //質問のジャンル
    bytes: ByteArray, //
    val answers: ArrayList<Answer> //Firebaseから取得した質問のモデルクラスであるAnswerのArrayList
) : Serializable {
    val imageBytes: ByteArray //Firebaseから取得した画像をbyte型の配列にしたもの

    init {
        imageBytes = bytes.clone() //
    }
}