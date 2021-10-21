package jp.techacademy.takafumi.shouji.autoslideshowapp

import android.Manifest
import android.content.ContentUris
import android.content.pm.PackageManager
import android.database.Cursor
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import jp.techacademy.takafumi.shouji.autoslideshowapp.databinding.ActivityMainBinding
import java.util.*

private const val PERMISSIONS_REQUEST_CODE = 100

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var cursor: Cursor
    private var mTimer: Timer? = null
    private var mHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        binding.forwardButton.setOnClickListener { onClickForwardButton() }
        binding.backButton.setOnClickListener { onClickBackButton() }
        binding.startStopButton.setOnClickListener { onClickStartStopButton() }
        binding.startStopButton.text = getString(R.string.LABEL_START)
        setContentView(binding.root)

        // 外部ストレージパーミッション確認
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android 6.0以降
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(
                        this,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    )
                ) {
                    // パーミッションが拒否されている状態:Toastを表示
                    Toast.makeText(this, "パーミッションが拒否状態です。", Toast.LENGTH_SHORT).show();
                } else {
                    // まだパーミッションの確認がされていない状態:確認ダイアログを表示
                    requestPermissions(
                        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                        PERMISSIONS_REQUEST_CODE
                    )
                }
                return
            }
        }

        // 画像情報を取得
        getContentsInfo()

        // データがあれば最初の画像を表示、何もなければclose
        if (cursor.moveToFirst()) {
            showImage()
        } else {
            cursor.close()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            PERMISSIONS_REQUEST_CODE ->
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getContentsInfo()
                }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    /**
     * 画像の情報を取得
     */
    private fun getContentsInfo() {
        val resolver = contentResolver
        cursor = resolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            null,
            null,
            null,
            null
        )!!
    }

    /**
     * 画像を表示
     */
    private fun showImage() {
        // indexからIDを取得し、そのIDから画像のURIを取得する
        val fieldIndex = cursor.getColumnIndex(MediaStore.Images.Media._ID)
        val id = cursor.getLong(fieldIndex)
        val imageUri =
            ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
        binding.imageView.setImageURI(imageUri)
    }

    /**
     * 進むボタンを押した時の動作
     */
    private fun onClickForwardButton() {
        // 次の画像を表示(最後の画像だった場合は最初の画像)
        if (cursor.isLast) {
            cursor.moveToFirst()
        } else {
            cursor.moveToNext()
        }
        showImage()
    }

    /**
     * 戻るボタンを押した時の動作
     */
    private fun onClickBackButton() {
        // 次の画像を表示(最後の画像だった場合は最初の画像)
        if (cursor.isFirst) {
            cursor.moveToLast()
        } else {
            cursor.moveToPrevious()
        }
        showImage()
    }

    /**
     * 再生/停止ボタンを押した時の動作
     */
    private fun onClickStartStopButton() {
        when (binding.startStopButton.text) {
            getString(R.string.LABEL_START) -> {
                // 進む、戻るボタン:使用不可にする
                binding.forwardButton.isEnabled = false
                binding.backButton.isEnabled = false
                // 再生/停止ボタンの文字を変更
                binding.startStopButton.text = getString(R.string.LABEL_STOP)
                if (mTimer == null) {
                    // スライドショー実行:2秒間隔
                    mTimer = Timer()
                    mTimer!!.schedule(object : TimerTask() {
                        override fun run() {
                            mHandler.post { onClickForwardButton() }
                        }
                    }, 2000, 2000)
                }
            }
            getString(R.string.LABEL_STOP) -> {
                if (mTimer != null) {
                    // スライドショー停止
                    mTimer!!.cancel()
                    mTimer = null
                }
                // 進む、戻るボタン:使用可にする
                binding.forwardButton.isEnabled = true
                binding.backButton.isEnabled = true
                // 再生/停止ボタンの文字を変更
                binding.startStopButton.text = getString(R.string.LABEL_START)
            }
            else -> Toast.makeText(this, "再生/停止処理にエラーが発生しました。", Toast.LENGTH_SHORT).show();
        }
    }
}