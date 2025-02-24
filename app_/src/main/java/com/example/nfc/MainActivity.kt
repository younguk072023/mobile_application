package com.example.nfc

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.yalantis.ucrop.UCrop
import okhttp3.*
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

class MainActivity : AppCompatActivity() {

    private lateinit var imageView: ImageView
    private var recentImageUri: Uri? = null
    private var cameraImageUri: Uri? = null
    private lateinit var radioButton: RadioButton
    private var unetFileData: ByteArray? = null
    private var isPreprocessed: Boolean = false
    private lateinit var resultButton: Button

    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>
    private lateinit var imagePickerLauncher: ActivityResultLauncher<Intent>
    private lateinit var permissionLauncher: ActivityResultLauncher<String>
    private lateinit var uCropLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        imageView = findViewById(R.id.imageView)
        radioButton = findViewById(R.id.RadioButton)
        resultButton = findViewById(R.id.button8)

        // 라디오 버튼 클릭 시 UNet 모델 로드
        radioButton.setOnClickListener {
            if (radioButton.isChecked) {
                unetFileData = loadUnetModelFromAssets("unet_645_180.pth")
                Toast.makeText(this, "UNet model loaded", Toast.LENGTH_SHORT).show()
            }
        }

        // result 버튼 클릭 시 서버로 이미지 전송
        resultButton.setOnClickListener {
            if (!isPreprocessed) {
                Toast.makeText(this, "Please preprocess the image first.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!radioButton.isChecked) {
                Toast.makeText(this, "Please select the UNet radio button.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (recentImageUri != null && unetFileData != null) {
                sendToServer("http://192.168.0.23:5000", recentImageUri!!, unetFileData!!)
            } else {
                Toast.makeText(this, "Image or UNet model not found.", Toast.LENGTH_SHORT).show()
            }
        }

        // 카메라 권한 요청
        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                openCamera()
            } else {
                Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show()
            }
        }

        // 카메라 버튼 클릭 시 권한 요청 후 카메라 열기
        val buttonCamera = findViewById<Button>(R.id.button)
        buttonCamera.setOnClickListener {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }

        // 카메라 실행 결과 처리
        cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                recentImageUri = cameraImageUri
                recentImageUri?.let {
                    imageView.setImageURI(it)
                    saveImageToGallery(it)
                }
            }
        }

        // 이미지 선택 버튼 클릭 이벤트
        val buttonChooseImage = findViewById<Button>(R.id.button4)
        buttonChooseImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            imagePickerLauncher.launch(intent)
        }

        // 이미지 선택 결과 처리
        imagePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK && result.data != null) {
                recentImageUri = result.data?.data
                recentImageUri?.let {
                    imageView.setImageURI(it)
                }
            } else {
                Toast.makeText(this, "Failed to pick image", Toast.LENGTH_SHORT).show()
            }
        }

        // UCrop 결과 처리 런처
        uCropLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK && result.data != null) {
                val resultUri = UCrop.getOutput(result.data!!)
                if (resultUri != null) {
                    val croppedBitmap = BitmapFactory.decodeStream(contentResolver.openInputStream(resultUri))
                    val grayscaleBitmap = convertToGrayscale(croppedBitmap)
                    imageView.setImageBitmap(grayscaleBitmap)
                    isPreprocessed = true

                    // Preprocessed 이미지를 저장하고 recentImageUri 갱신
                    val preprocessedUri = savePreprocessedImage(grayscaleBitmap)
                    if (preprocessedUri != null) {
                        recentImageUri = preprocessedUri
                    }
                }
            } else if (result.resultCode == UCrop.RESULT_ERROR) {
                val cropError = UCrop.getError(result.data!!)
                cropError?.printStackTrace()
                Toast.makeText(this, "Crop failed: ${cropError?.message}", Toast.LENGTH_SHORT).show()
            }
        }

        // Preprocessing 버튼 클릭 시 크롭 및 그레이스케일 처리
        val buttonPreprocessing = findViewById<Button>(R.id.button6)
        buttonPreprocessing.setOnClickListener {
            recentImageUri?.let {
                startCrop(it)
            } ?: run {
                Toast.makeText(this, "Please choose or capture an image first", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendToServer(serverUrl: String, imageUri: Uri, unetData: ByteArray) {
        val client = OkHttpClient()

        try {
            val imageFile = File(cacheDir, "preprocessed_image.png")  // PNG 파일 전송
            if (!imageFile.exists()) {
                runOnUiThread {
                    Toast.makeText(this, "Preprocessed image not found", Toast.LENGTH_SHORT).show()
                }
                return
            }

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("image", imageFile.name, RequestBody.create(MediaType.parse("image/png"), imageFile))  // image/png로 설정
                .addFormDataPart("unet_model", "unet.pth", RequestBody.create(MediaType.parse("application/octet-stream"), unetData))
                .build()

            val request = Request.Builder()
                .url(serverUrl + "/segment")
                .post(requestBody)
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    e.printStackTrace()
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "Failed to send to server: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    runOnUiThread {
                        if (response.isSuccessful) {
                            val jsonResponse = response.body()?.string()
                            val segmentedImageFilename = JSONObject(jsonResponse).getString("segmented_image_filename")

                            // 받은 파일 이름으로 이미지를 다운로드
                            downloadSegmentedImage(serverUrl, segmentedImageFilename)
                        } else {
                            Toast.makeText(this@MainActivity, "Failed to send to server: ${response.code()}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            })
        } catch (e: Exception) {
            e.printStackTrace()
            runOnUiThread {
                Toast.makeText(this, "Error preparing files for upload: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 기존 downloadSegmentedImage 함수 수정 - 갤러리에 저장하도록 추가
    private fun downloadSegmentedImage(serverUrl: String, segmentedImageFilename: String) {
        val client = OkHttpClient()

        val request = Request.Builder()
            .url("$serverUrl/uploads/$segmentedImageFilename?timestamp=${System.currentTimeMillis()}")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Failed to download segmented image: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val inputStream = response.body()?.byteStream()
                    val bitmap = BitmapFactory.decodeStream(inputStream)

                    if (bitmap != null) {
                        runOnUiThread {
                            imageView.setImageBitmap(bitmap)
                            saveToGallery(bitmap)
                            Toast.makeText(this@MainActivity, "Segmented image downloaded and saved to gallery", Toast.LENGTH_SHORT).show()

                            // UNet 라디오 버튼 선택 해제
                            radioButton.isChecked = false
                        }
                    } else {
                        runOnUiThread {
                            Toast.makeText(this@MainActivity, "Failed to decode the image", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "Failed to download segmented image: ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }


    // 새로 추가된 saveToGallery 함수 - 다운로드된 이미지를 갤러리에 저장
    private fun saveToGallery(bitmap: Bitmap) {
        val filename = "Segmented_IMG_${System.currentTimeMillis()}.png"
        var outputStream: FileOutputStream? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/SegmentedImages")
            }
            val contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            val imageUri = contentResolver.insert(contentUri, values)
            outputStream = imageUri?.let { contentResolver.openOutputStream(it) } as FileOutputStream
        } else {
            val imagesDir = File(getExternalFilesDir(null), "SegmentedImages")
            if (!imagesDir.exists()) {
                imagesDir.mkdirs()
            }
            val imageFile = File(imagesDir, filename)
            outputStream = FileOutputStream(imageFile)
        }

        outputStream?.let {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            it.flush()
            it.close()
        }
    }

    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val photoFile = File(externalCacheDir, "camera_photo.jpg")
        cameraImageUri = FileProvider.getUriForFile(this, "$packageName.fileprovider", photoFile)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri)
        cameraLauncher.launch(intent)
    }

    private fun startCrop(uri: Uri) {
        val destinationUri = Uri.fromFile(File(cacheDir, "croppedImage.jpg"))
        val uCrop = UCrop.of(uri, destinationUri)
            .withAspectRatio(645f, 180f)
            .withMaxResultSize(645, 180)

        val uCropIntent = uCrop.getIntent(this)
        uCropLauncher.launch(uCropIntent)
    }

    private fun loadUnetModelFromAssets(fileName: String): ByteArray? {
        return try {
            val inputStream: InputStream = assets.open(fileName)
            inputStream.readBytes()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun convertToGrayscale(bitmap: Bitmap): Bitmap {
        val grayscaleBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(grayscaleBitmap)
        val paint = android.graphics.Paint()
        val colorMatrix = android.graphics.ColorMatrix()
        colorMatrix.setSaturation(0f)
        val filter = android.graphics.ColorMatrixColorFilter(colorMatrix)
        paint.colorFilter = filter
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return grayscaleBitmap
    }

    private fun saveImageToGallery(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            var bitmap = inputStream?.let { BitmapFactory.decodeStream(it) }

            if (bitmap != null) {
                bitmap = rotateImageIfRequired(bitmap, uri)

                val filename = "IMG_${System.currentTimeMillis()}.png"  // 확장자를 .png로 변경
                var outputStream: FileOutputStream? = null
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val values = ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                        put(MediaStore.Images.Media.MIME_TYPE, "image/png")  // MIME 타입을 image/png로 변경
                        put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/Camera")
                    }
                    val contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    val imageUri = contentResolver.insert(contentUri, values)
                    outputStream = imageUri?.let { contentResolver.openOutputStream(it) } as FileOutputStream
                } else {
                    val imagesDir = File(getExternalFilesDir(null), "Camera")
                    if (!imagesDir.exists()) {
                        imagesDir.mkdirs()
                    }
                    val imageFile = File(imagesDir, filename)
                    outputStream = FileOutputStream(imageFile)
                }

                outputStream?.let {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)  // PNG 형식으로 저장
                    it.flush()
                    it.close()
                    Toast.makeText(this, "Image saved to gallery", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Failed to decode bitmap", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to save image", Toast.LENGTH_SHORT).show()
        }
    }

    private fun savePreprocessedImage(grayscaleBitmap: Bitmap): Uri? {
        return try {
            val preprocessedFile = File(cacheDir, "preprocessed_image.png")  // PNG로 저장
            val outputStream = FileOutputStream(preprocessedFile)
            val isSaved = grayscaleBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)  // PNG 형식으로 저장
            outputStream.flush()
            outputStream.close()

            if (isSaved) {
                Uri.fromFile(preprocessedFile)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun rotateImageIfRequired(bitmap: Bitmap, uri: Uri): Bitmap {
        val inputStream = contentResolver.openInputStream(uri)
        val exif = androidx.exifinterface.media.ExifInterface(inputStream!!)
        val orientation = exif.getAttributeInt(androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION, androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL)

        return when (orientation) {
            androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_90 -> rotateImage(bitmap, 90f)
            androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_180 -> rotateImage(bitmap, 180f)
            androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_270 -> rotateImage(bitmap, 270f)
            else -> bitmap
        }
    }

    private fun rotateImage(img: Bitmap, degree: Float): Bitmap {
        val matrix = android.graphics.Matrix()
        matrix.postRotate(degree)
        return Bitmap.createBitmap(img, 0, 0, img.width, img.height, matrix, true)
    }
}
