package jp.techacademy.otowa.kimura.qa_app

import java.io.Serializable

class Answer(
    val body: String, //回答者の本文
    val name: String, //回答者の名前
    val uid: String, //回答者のUID
    val answerUid: String)//回答UID
    : Serializable