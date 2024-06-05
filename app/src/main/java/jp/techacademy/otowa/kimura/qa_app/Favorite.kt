package jp.techacademy.otowa.kimura.qa_app

import java.io.Serializable

class Favorite(
    val questionUid: String = "", // 質問のUID
    val genre: Int = 0// 質問のジャンル
) : Serializable