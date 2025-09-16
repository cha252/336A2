package com.example.hada_a2

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.hada_a2.ui.theme.Hada_A2Theme
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.collection.LruCache
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    //Declare variables
    private var hasPermission by mutableStateOf(false)
    private lateinit var memoryCache: LruCache<String, Bitmap>
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        hasPermission = isGranted
    }

    //Function to request appropriate permission based on device
    private fun checkAndRequestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
            }
            else{
                hasPermission = true
            }
        } else {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            else{
                hasPermission = true
            }
        }
    }

    //Function to get images from device memory
    fun getImages(context: Context): List<Uri>{
        //Declare variables
        val uris = mutableListOf<Uri>()
        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projectionn = arrayOf(MediaStore.Images.Media._ID)

        //Add all images to uris in date order (newest first)
        context.contentResolver.query(
            collection,
            projectionn,
            null,
            null,
            "${MediaStore.Images.Media.DATE_ADDED} DESC"
        )?.use{ cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)

            while(cursor.moveToNext()){
                val id = cursor.getLong(idColumn)
                val contentUri = Uri.withAppendedPath(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    id.toString()
                )
                uris.add(contentUri)
            }
        }

        return uris
    }

    //Lazy vertical grid to display the images
    @Composable
    fun CameraRoll(modifier: Modifier = Modifier) {
        //Declare variables
        val context = LocalContext.current
        var imageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }

        //Check for permission
        LaunchedEffect(hasPermission) { if(hasPermission) { imageUris = getImages(context) } }

        //If the app has the required permission
        if(hasPermission) {
            //Set the lazy vertical grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(8.dp),
                modifier = modifier.fillMaxSize()
            ) {
                //Set each item of the grid as a thumbnail
                items(imageUris.size) { index ->
                    ThumbnailImage(imageUris[index])
                }
            }
        }
        //If permission has not been granted
        else {
            //Show some text to say that the permission is required
            Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Permission required to show images")
            }
        }
    }


    @Composable
    //Thumbnail function to get a thumbnail of each image
    fun ThumbnailImage(uri: Uri) {
        //Declare variables
        val context = LocalContext.current
        var bitmap by remember { mutableStateOf<Bitmap?>(null) }

        LaunchedEffect(uri) {
            //Use the uri string as the key
            val key = uri.toString()

            //Check if in cache first
            val cached = (context as MainActivity).memoryCache.get(key)
            //If the image has already been cached
            if(cached != null){
                //Set bitmap to the cached image
                bitmap = cached
            }
            else {
                //Use a coroutine to get the thumbnail
                withContext(Dispatchers.IO) {
                    try {
                        context.contentResolver.openInputStream(uri)?.use{stream ->
                            val original = BitmapFactory.decodeStream(stream)
                            val thumbnail = Bitmap.createScaledBitmap(original, 400, 400, true)
                            bitmap = thumbnail
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }

        //Return a box containing the thumbnail
        Box(
            modifier = Modifier
                .padding(2.dp)
                .aspectRatio(1f)
                .fillMaxWidth()
                .background(Color.LightGray)
        ) {
            bitmap?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxHeight()
                )
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //Set up cache size
        val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
        val cacheSize = maxMemory / 8
        memoryCache = object: LruCache<String, Bitmap>(cacheSize){
            override fun sizeOf(key: String, value: Bitmap): Int {
                return value.byteCount / 1024
            }
        }
        //Check for the permission and get if required
        checkAndRequestPermission()

        enableEdgeToEdge()
        setContent {
            Hada_A2Theme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    CameraRoll(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    @Preview(showBackground = true, showSystemUi = true)
    @Composable
    fun CameraRollPreview() {
        Hada_A2Theme {
            CameraRoll()
        }
    }
}