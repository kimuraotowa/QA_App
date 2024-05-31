package jp.techacademy.otowa.kimura.qa_app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import jp.techacademy.otowa.kimura.qa_app.databinding.ListAnswerBinding
import jp.techacademy.otowa.kimura.qa_app.databinding.ListQuestionDetailBinding

//listViewのアダプターとして機能
class QuestionDetailListAdapter(context: Context, private val question: Question) : BaseAdapter() {
    companion object {
        //どのレイアウトを使って表示するかを判断するためのタイプを表す定数。
        //質問タイプ
        private const val TYPE_QUESTION = 0
        //回答タイプ
        private const val TYPE_ANSWER = 1
    }

    private var layoutInflater: LayoutInflater

    //LayoutInflaterの初期化(XMLをViewに変換)
    init {
        layoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    }

    //リストViewのアイテム数を返す。
    override fun getCount(): Int {
        //質問1つと回答の数を合計した数を返す。
        //リストViewに表示するアイテムの数を指定するため
        return 1 + question.answers.size
    }

    //指定された位置のアイテムの対応を返す
    //最初のアイテムは質問、それいがいは回答
    //アイテムの種類に応じて異なるレイアウトを適用するため
    override fun getItemViewType(position: Int): Int {
        return if (position == 0) {
            TYPE_QUESTION
        } else {
            TYPE_ANSWER
        }
    }

    //リストViewに表示されるアイテムのタイプ数を返す
    //質問か、回答か
    override fun getViewTypeCount(): Int {
        return 2
    }

    //指定された位置のアイテムを返す
    //常にクエッションを返す
    //リストViewのアイテムを取得するため
    override fun getItem(position: Int): Any {
        return question
    }

    //指定された位置のアイテムのIDを返す
    //常に0にする
    override fun getItemId(position: Int): Long {
        return 0
    }

    //指定されたアイテムのViewを返す
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        //質問どうかを判断する
        if (getItemViewType(position) == TYPE_QUESTION) {
            // ViewBindingを使うための設定
            //質問のViewを作成または再利用
            val binding = if (convertView == null) {
                ListQuestionDetailBinding.inflate(layoutInflater, parent, false)
            } else {
                ListQuestionDetailBinding.bind(convertView)
            }
            //
            val view: View = convertView ?: binding.root

            binding.bodyTextView.text = question.body
            binding.nameTextView.text = question.name

            //質問に画像があるあ場合に表示するため
            val bytes = question.imageBytes
            if (bytes.isNotEmpty()) {
                val image = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    .copy(Bitmap.Config.ARGB_8888, true)
                binding.imageView.setImageBitmap(image)
            }

            return view

        } else {
            // ViewBindingを使うための設定
            val binding = if (convertView == null) {
                ListAnswerBinding.inflate(layoutInflater, parent, false)
            } else {
                ListAnswerBinding.bind(convertView)
            }
            val view: View = convertView ?: binding.root

            //回答の本文と名前を設定
            binding.bodyTextView.text = question.answers[position - 1].body
            binding.nameTextView.text = question.answers[position - 1].name

            //回答のViewを返す
            return view
        }
    }
}