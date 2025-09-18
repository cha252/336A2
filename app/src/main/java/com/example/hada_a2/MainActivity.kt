//Chisora Hada
//23001770

package com.example.hada_a2

import android.Manifest
import android.annotation.SuppressLint
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
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.collection.LruCache
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring.DampingRatioLowBouncy
import androidx.compose.animation.core.Spring.StiffnessVeryLow
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.math.min
import kotlin.math.max

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
            } else {
                hasPermission = true
            }
        } else {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            } else {
                hasPermission = true
            }
        }
    }

    //Function to get images from device memory
    fun getImages(context: Context): List<Uri> {
        //Declare variables
        val uris = mutableListOf<Uri>()
        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(MediaStore.Images.Media._ID)

        //Add all images to uris in date order (newest first)
        context.contentResolver.query(
            collection,
            projection,
            null,
            null,
            "${MediaStore.Images.Media.DATE_ADDED} DESC"
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val contentUri = Uri.withAppendedPath(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    id.toString()
                )
                uris.add(contentUri)
            }
        }

        //Return list of images' uris
        return uris
    }

    //Calculate in sample image size copied from https://developer.android.com/topic/performance/graphics/load-bitmap#kotlin
    fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    //Function to fix the rotation of an image
    fun fixRotation(context: Context, uri: Uri, decoded: Bitmap?): Bitmap? {
        //Get orientation of the bitmap
        val exif = ExifInterface(context.contentResolver.openInputStream(uri)!!)
        val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)

        //Rotate image's orientation if wrong
        val rotMatrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> { rotMatrix.postRotate(90f) }
            ExifInterface.ORIENTATION_ROTATE_180 -> { rotMatrix.postRotate(180f) }
            ExifInterface.ORIENTATION_ROTATE_270 -> { rotMatrix.postRotate(270f) }
        }

        //Rotate bitmap
        return Bitmap.createBitmap(decoded!!, 0, 0, decoded.width, decoded.height, rotMatrix, true)
    }

    //Composable to display the selected photo
    @Composable
    fun SinglePhoto(uri: Uri){
        //Get context and declare a bitmap for the photo
        val context = LocalContext.current
        var bitmap by remember { mutableStateOf<Bitmap?>(null) }

        //Values for zooming and navigating the image
        var scale by remember { mutableFloatStateOf(1f) }
        var offsetX by remember { mutableFloatStateOf(0f) }
        var offsetY by remember { mutableFloatStateOf(0f) }
        var width by remember { mutableStateOf(0) }
        var height by remember { mutableStateOf(0) }

        //Get the selected photo
        LaunchedEffect(uri) {
            CoroutineScope(Dispatchers.IO).launch {
                val stream = context.contentResolver.openInputStream(uri)
                bitmap = BitmapFactory.decodeStream(stream)
                //Fix rotation of image
                bitmap = fixRotation(context, uri, bitmap)
                //Get image dimensions
                width = bitmap!!.width
                height = bitmap!!.height
            }
        }
        //Display the image in a box
        Box(
            modifier = Modifier.size(width.dp, height.dp),
            contentAlignment = Alignment.Center
        ){
            bitmap?.let{
                //Update the zoom and offset of the image (calculations based on https://www.youtube.com/watch?v=3CjOyoqi_PQ)
                val state = rememberTransformableState { zoomChange, offsetChange, rotationChange ->
                    scale = (scale * zoomChange).coerceIn(1f, 10f)

                    val extraWidth = (scale - 1) * width
                    val extraHeight = (scale - 1) * height

                    val maxX = extraWidth / 2
                    val maxY = extraHeight / 2

                    offsetX = (offsetX + offsetChange.x).coerceIn(-maxX, maxX)
                    offsetY = (offsetY + offsetChange.y).coerceIn(-maxY, maxY)

                }
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = offsetX
                            translationY = offsetY
                        }
                        .transformable(state)
                )
            }
        }
    }

    //Lazy vertical grid to display the images
    @Composable
    fun CameraRoll(modifier: Modifier = Modifier, navController: NavController) {
        //Declare variables
        val context = LocalContext.current
        var imageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }

        //Scale to define how zoomed in the photos should be
        var scaleState by remember { mutableFloatStateOf(1f) }
        var columns by remember { mutableIntStateOf(2) }

        //Use animateFloatAsState to animate the scale
        val scale by animateFloatAsState(
            targetValue = scaleState,
            animationSpec = spring(dampingRatio = DampingRatioLowBouncy, stiffness = StiffnessVeryLow)
        )

        //Check for permission
        LaunchedEffect(hasPermission) { if(hasPermission) { imageUris = getImages(context) } }

        //If the app has the required permission
        if(hasPermission) {
            //Put the lazy vertical grid in a box
            Box(
                //Pinch to zoom copied from MatchingGridViewModel example
                Modifier.pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(pass = PointerEventPass.Initial)
                        do {
                            val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                            val zoomChange = event.calculateZoom()
                            if (zoomChange != 1f) {
                                scaleState *= zoomChange
                                if(scale!=0f) {
                                    val cols =
                                        min(max((columns / scale).roundToInt(), 1), 6)
                                    if (cols != columns) {
                                        columns = cols
                                    }
                                }
                                event.changes.forEach { it.consume() }
                            }
                        } while (event.changes.any { it.pressed })
                        scaleState = 1f
                    }
                }
            ){
                //Set the lazy vertical grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    contentPadding = PaddingValues(10.dp),
                    modifier = modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale
                        )
                ) {
                    //Set each item of the grid as a thumbnail
                    items(imageUris.size) { index ->
                        ThumbnailImage(imageUris[index], navController)
                    }
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
    fun ThumbnailImage(uri: Uri, navController: NavController) {
        //Declare variables
        val context = LocalContext.current
        var bitmap by remember { mutableStateOf<Bitmap?>(null) }

        LaunchedEffect(uri) {
            //Get the uri as a string and whether the image has been cached or not
            val key = uri.toString()
            val cached = (context as MainActivity).memoryCache[key]

            //If image has already been cached
            if (cached != null) {
                //Set the bitmap to the cached value
                bitmap = cached
            } else {
                //If the image has not been cached, use a coroutine to get the thumbnail
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        //Decode with inJustDecodeBounds=true to check dimensions
                        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            BitmapFactory.decodeStream(stream, null, options)
                        }

                        //Calculate the inSampleSize
                        options.inSampleSize = calculateInSampleSize(options, 400, 400)

                        //Decode bitmap with inSampleSize set
                        options.inJustDecodeBounds = false
                        val decoded = context.contentResolver.openInputStream(uri)?.use { stream ->
                            BitmapFactory.decodeStream(stream, null, options)
                        }

                        //Fix orientation
                        val rotatedBitmap = fixRotation(context, uri, decoded)

                        //Add the decoded bitmap to the cache
                        rotatedBitmap?.let {
                            bitmap = it
                            context.memoryCache.put(key, it)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }

        //Use animated visibility to add animation when photo comes onto screen
        AnimatedVisibility(
            visible = bitmap != null,
            enter = scaleIn()
        ) {
            //Uri as a string to pass to the nav controller
            val uriString = Uri.encode(uri.toString())

            //Make the thumbnail a clickable box
            Box(
            modifier = Modifier
                .padding(2.dp)
                .aspectRatio(1f)
                .fillMaxWidth()
                .background(Color.LightGray)
                .clickable { navController.navigate("SinglePhoto/$uriString") }
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
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    //App bar composable based on MatchGridViewModel example
    fun AppBar(){
        TopAppBar(
            title = { Text("Gallery") },
            actions = {
                IconButton(onClick = {
//                    do something here
                }) {
                    Icon(Icons.Filled.Refresh, null)
                }
            }
        )
    }

    @Composable
    fun Init(){
    }

    @Composable
    fun AppUI(modifier: Modifier = Modifier){
        //Navigation controller and host to navigate between the gallery and individual images
        val navController = rememberNavController()
        NavHost(navController = navController, startDestination = "CameraRoll", builder = {
            //Define the camera roll composable
            composable("CameraRoll"){
               CameraRoll(modifier, navController)
            }
            //Define the composable for each image
            composable("SinglePhoto"+"/{uri}") {
                val uriString = it.arguments?.getString("uri")
                val uri = Uri.parse(Uri.decode(uriString))
                SinglePhoto(uri)
            }
        })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //Set up cache size
        val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
        val cacheSize = maxMemory / 8
        memoryCache = object: LruCache<String, Bitmap>(cacheSize) {
            override fun sizeOf(key: String, value: Bitmap): Int {
                return value.byteCount / 1024
            }
        }

        //Check for the permission and get if required
        checkAndRequestPermission()

        //Set content of app
        enableEdgeToEdge()
        setContent {
            Hada_A2Theme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = { AppBar() }
                ) { innerPadding ->
                    AppUI(Modifier.padding(innerPadding))
                }
            }
        }
    }

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @Preview(showBackground = true, showSystemUi = true)
    @Composable
    fun CameraRollPreview() {
        Hada_A2Theme {
            Scaffold(
                topBar = { AppBar() },
            ) {
                val modifier = Modifier.padding(it)
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = "CameraRoll", builder = {
                    composable("CameraRoll"){
                        CameraRoll(modifier, navController)
                    }
                    composable("SinglePhoto"+"/{uri}") {
                        val uri = Uri.parse(it.arguments?.getString("uri"))
                        SinglePhoto(uri)
                    }
                })
                CameraRoll(navController = navController)
            }
        }
    }
}
//Need to add swipe to go back once in image
//Need to make refresh button functional