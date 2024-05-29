package jp.techacademy.otowa.kimura.qa_app

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.preference.PreferenceManager
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import jp.techacademy.otowa.kimura.qa_app.databinding.ActivityQuestionSendBinding
import java.io.ByteArrayOutputStream

// View.OnClickListener：クリックイベントonClickをオーバーライドしないといけない。
//DatabaseReference.CompletionListener:Firebase データベース操作の完了を処理する。
class QuestionSendActivity : AppCompatActivity(), View.OnClickListener,
    DatabaseReference.CompletionListener {

    //PERMISSIONS_REQUEST_CODE は、
    // パーミッションリクエストのコードとして使用される定数
    //= 100：他の値でも問題ない
    companion object {
        private const val PERMISSIONS_REQUEST_CODE = 100
    }

    private lateinit var binding: ActivityQuestionSendBinding

    //genre: 質問のジャンルを保存するための整数型の変数
    private var genre: Int = 0
    //pictureUri: 選択された画像の URI を保存するための変数
    private var pictureUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQuestionSendBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 渡ってきたジャンルの番号を保持する
        //intent.extras から渡されたジャンルの番号を取得し、genre変数に格納します。
        val extras = intent.extras
        genre = extras!!.getInt("genre")

        // UIの準備
        //Activityのタイトルの設定
        title = getString(R.string.question_send_title)

        //Viewにクリックリスナーを設定してる
        // ボタンと画像Viewをクリックしたときにイベントの処理をする
        binding.sendButton.setOnClickListener(this)
        binding.imageView.setOnClickListener(this)
    }


      //このActivityに戻ってきた時の処理
     //registerForActivityResult：コールバックを登録するメソッド
    private var launcher = registerForActivityResult(
        //他のアクティビティを開始し、その結果を受け取るための契約（Contract）
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
          //resultCode:Activityの結果のコード
        val resultCode: Int = result.resultCode
          //data:Activityから返されたIntent
        val data: Intent? = result.data

          //Activity.RESULT_OK：アクティビティが正常に終了したことを示してる
        if (resultCode != Activity.RESULT_OK) {
            //結果コードが Activity.RESULT_OK でない場合、
            // pictureUriが存在すれば、そのURIを削除し、
            // pictureUriをnull に設定して処理を終了します。
            if (pictureUri != null) {
                contentResolver.delete(pictureUri!!, null, null)
                pictureUri = null
            }
            return@registerForActivityResult
        }

        // 画像を取得
         // dataがnullまたはdata.dataがnullの場合, pictureUri を使用します。
          // それ以外の場合は、data.data を使用します。
        val uri = if (data == null || data.data == null) pictureUri else data.data

        // URIからBitmapを取得する
        val image: Bitmap
        try {
            val contentResolver = contentResolver
            //contentResolver.openInputStream(uri!!):URIから入力ストリームを開きます。
            val inputStream = contentResolver.openInputStream(uri!!)
            //BitmapFactory.decodeStream(inputStream):入力ストリームからBitmapを元の形式に戻します
            image = BitmapFactory.decodeStream(inputStream)
            //入力ストリームを閉じます。
            inputStream?.close()

            //例外が発生した場合、処理を終了する
        } catch (e: Exception) {
            return@registerForActivityResult
        }

        // 取得したBimapの長辺を500ピクセルにリサイズする
          //画像の幅
        val imageWidth = image.width
          //画像の高さ
        val imageHeight = image.height
          //長方形を500にするための計算
          //小さい方のスケールを選択
        val scale =
            (500.toFloat() / imageWidth).coerceAtMost(500.toFloat() / imageHeight) // (1)

          //スケールの設定
        val matrix = Matrix()
        matrix.postScale(scale, scale)

          //元のBitmapを
          //0, 0,:座標、幅と高さ (imageWidth, imageHeight)、
          //matrix:画像のスケーリング、回転お重なうためのオブジェクト
          //true:スケーリング時にフィルタリングを行うかどうかを指定。
        val resizedImage =
            Bitmap.createBitmap(image, 0, 0, imageWidth, imageHeight, matrix, true)

        // リサイズされたBitmapをImageViewに設定する
        binding.imageView.setImageBitmap(resizedImage)

          //nullに設定
        pictureUri = null
    }

    override fun onClick(v: View) {
        if (v === binding.imageView) {
            //画像Viewがクリックされたとき。
            //Build.VERSION_CODES.TIRAMISU:Androidバージョンの確認
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // 許可されている
                showChooser()
            } else {
                //WRITE_EXTERNAL_STORAGE： パーミッションの許可状態を確認する
                if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    // 許可されている
                    //showChooser:画像選択ダイアログを表示
                    showChooser()
                } else {
                    //許可されていない場合
                    // requestPermissions：パーミッションリクエストダイアログを表示
                    requestPermissions(
                        arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                        PERMISSIONS_REQUEST_CODE
                    )

                    return
                }
            }
            //送信ボタンがクリックされたとき
        } else if (v === binding.sendButton) {
            // キーボードが出てたら閉じる
            val im = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            im.hideSoftInputFromWindow(v.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS)

            //FirebaseDatabase.getInstance().reference:Firebaseデータ参照
            val dataBaseReference = FirebaseDatabase.getInstance().reference

            //ContentsPATH:特定のデータの取得
            val genreRef = dataBaseReference.child(ContentsPATH).child(genre.toString())

            //データの準備
            //HashMap：送信するデータの準備//1
            val data = HashMap<String, String>()

            // UID//2
            data["uid"] = FirebaseAuth.getInstance().currentUser!!.uid

            // タイトルと本文を取得する//3
            val title = binding.titleText.text.toString()
            val body = binding.bodyText.text.toString()

            if (title.isEmpty()) {
                // タイトルが入力されていない時はエラーを表示するだけ
                Snackbar.make(v, getString(R.string.input_title), Snackbar.LENGTH_LONG).show()
                return
            }

            if (body.isEmpty()) {
                // 質問が入力されていない時はエラーを表示するだけ
                Snackbar.make(v, getString(R.string.question_message), Snackbar.LENGTH_LONG).show()
                return
            }

            // Preferenceから名前を取る//4
            val sp = PreferenceManager.getDefaultSharedPreferences(this)
            val name = sp.getString(NameKEY, "")

            data["title"] = title
            data["body"] = body
            data["name"] = name!!

            //1,2,3,4 を取得してデータにい追加

            // 添付画像を取得する
            //BitmapDrawable:画像取得をキャスト
            val drawable = binding.imageView.drawable as? BitmapDrawable

            // 添付画像が設定されていれば、
            if (drawable != null) {
                //Bitmapを取得し、
                val bitmap = drawable.bitmap
                val byteArrayOutputStream = ByteArrayOutputStream()
                //JPEG形式で圧縮して
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream)
                //BASE64エンコードする
                val bitmapString =
                    Base64.encodeToString(byteArrayOutputStream.toByteArray(), Base64.DEFAULT)

                //エンコードされた画像データをデータに追加する。
                data["image"] = bitmapString
            }

            //Firebaseに送信
            genreRef.push().setValue(data, this)
            //プログレスバー表示
            binding.progressBar.visibility = View.VISIBLE
        }
    }

    //onRequestPermissionsResult:パーミッションリクエストに対応した後に呼ばれるコールバックメソッド
    override fun onRequestPermissionsResult(
        //パーミッションリクエストを識別するためのコード
        //リクエストを行った際のコードがくる
        requestCode: Int,

        //リクエストされたパーミッションの配列
        //複数のパーミッションを１度でリクエストできる。
        permissions: Array<String>,

        //パーミッションの結果を示す配列
        //PackageManager.PERMISSION_GRANTED または
        // PackageManager.PERMISSION_DENIED のいずれかの値が含まれます。
        grantResults: IntArray

    ) {
        //onRequestPermissionsResult:親クラスを呼び出して基本的な処理をする
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        //requestCode:を使ってどのパーミッションリクエストに対する結果であるかを確認
        when (requestCode) {
            //この場合PERMISSIONS_REQUEST_CODEに対する結果
            PERMISSIONS_REQUEST_CODE -> {
                //grantResults[0]:ユーザーが許可したかどうか
                //PackageManager.PERMISSION_GRANTED]:許可したことを示す
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // ユーザーが許可したときの処理
                    showChooser()
                }
                return
            }
        }
    }

    //ギャラリーから選択するか、カメラで撮影を行うかエレブことができる
    private fun showChooser() {
        //Intent.ACTION_GET_CONTENT: ギャラリーから選択するIntent
        val galleryIntent = Intent(Intent.ACTION_GET_CONTENT)
        //画像ファイルのみを対象とするためのフィルタ
        galleryIntent.type = "image/*"
        //開くことができるファイルのみ対象としている。
        galleryIntent.addCategory(Intent.CATEGORY_OPENABLE)

        // カメラで撮影するIntent
        //ファイル名の作成
        val filename = System.currentTimeMillis().toString() + ".jpg"
        //画像のタイプと、MiMEのタイプ設定
        val values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE, filename)
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        //contentResolver.insert：新しい画像のURI取得。
        // このURL:カメラで撮影したURIのこと
        pictureUri = contentResolver
            .insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

        //カメラアプリ起動のIntent
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

        //撮影した画像を指定したURIに保存するようにカメラアプリに指示。
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, pictureUri)

        // ギャラリー選択のIntentを与えてcreateChooserメソッドを呼ぶ
        //Intent.createChooser:ユーザーに選択肢を提供するためのインテント作成
        // getString(R.string.get_image)):選択ダイアログのタイトルを設定
        val chooserIntent = Intent.createChooser(galleryIntent, getString(R.string.get_image))

        // EXTRA_INITIAL_INTENTSにカメラ撮影のIntentを追加
        // ユーザーはギャラリーから画像を選択するか、カメラで新しい画像を撮影するかを選べる
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(cameraIntent))

        //launcher.launch:事前に登録しているActivity結果をランチャーを使って、選択ダイアログを起動。
        //launcher(ランチャー):Activityや操作を開始するための仕組み
        launcher.launch(chooserIntent)
    }

    //Firebaseにデータが送信完了時に呼び出される。コールバックメソッド
    // データベースの操作が成功した場合nullになる
    override fun onComplete(databaseError: DatabaseError?, databaseReference: DatabaseReference) {
       //データ送信が完了したため、プロブレスバーを非表示
        binding.progressBar.visibility = View.GONE

     //エラーチェック
        //databaseErrorがnullであれば、データが成功
        //null出なければ、データ送信が失敗
        //成功した場合Activityを終了する
        if (databaseError == null) {
            finish()
        } else {
            //失敗した場合エラーメッセージの表示
            Snackbar.make(
                //ActivityのルートView取得
                findViewById(android.R.id.content),
                //エラーメッセージ表示
                getString(R.string.question_send_error_message),
                //時間設定
                Snackbar.LENGTH_LONG
            ).show()
        }
    }
}