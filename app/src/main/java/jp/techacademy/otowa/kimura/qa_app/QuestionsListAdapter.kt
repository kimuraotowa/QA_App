package jp.techacademy.otowa.kimura.qa_app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import jp.techacademy.otowa.kimura.qa_app.databinding.ListQuestionsBinding

//listViewのアダプターとして機能
//
class QuestionsListAdapter(context: Context) : BaseAdapter() {
    //XMLレイアウトファイルをViewobjectに変換する際に使用する
    private var layoutInflater: LayoutInflater
    //質問リストを保存するためのArrayList<Question>です
    private var questionArrayList = ArrayList<Question>()

    //初期化
    init {
        layoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    }

    //ここではlistViewに表示するサイズを返してる
    override fun getCount(): Int {
        return questionArrayList.size
    }

    // 指定された位置にあるアイテムを返します。
    //ここでは質問リストの特定の質問に返す
    override fun getItem(position: Int): Any {
        return questionArrayList[position]
    }

    //指定された位置にあるアイテムのIDを返します。
    //ここでは位置をそのままIDとして返す。
    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        // ViewBindingを使うための設定
        //nullの場合新しいViewを作成
        val binding = if (convertView == null) {
            //ListQuestionsBinding:Viewのbindingを行う
            ListQuestionsBinding.inflate(layoutInflater, parent, false)
        } else {
            //convertViewがnullの場合新しいbindingを作成し
            // 、そうでない場合は、今あるViewにbindingする
            ListQuestionsBinding.bind(convertView)
        }
        val view: View = convertView ?: binding.root

        //TextViewに設定（タイトル、名前、回答）
        binding.titleTextView.text = questionArrayList[position].title
        binding.nameTextView.text = questionArrayList[position].name
        binding.resTextView.text = questionArrayList[position].answers.size.toString()

        //質問に関する画像表示するための処理
        //questionArrayList の指定された位置（position）にある質問オブジェクトから imageBytes プロパティを取得します
        //imageBytes は、画像データをバイト配列として保存してる
        val bytes = questionArrayList[position].imageBytes
        //画像データのバイト配列が空でないかを確認します。
        // 空でない場合にのみ、画像のデコードと表示を行います。
        if (bytes.isNotEmpty()) {
            //BitmapFactory クラスの decodeByteArray メソッドを使用して、
            // バイト配列から Bitmap オブジェクトを生成します。
            //第1引数：バイト配列 (bytes)
            //第2引数：デコードを開始する位置（ここでは0
            //第3引数：デコードするバイト数（ここではバイト配列のサイズ）
            val image = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                .copy(Bitmap.Config.ARGB_8888, true)

            //生成された Bitmap を ImageView に設定します。
            // これにより、画像が ImageView に表示されます。
            binding.imageView.setImageBitmap(image)
        }

        return view
    }

    //質問リストを設定する為のメソッド、新しい質問を受け取り、内部のリストを更新します。
    fun setQuestionArrayList(questionArrayList: ArrayList<Question>) {
        this.questionArrayList = questionArrayList
    }
}
