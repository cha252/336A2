package com.example.hada_a2

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
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.internal.throwMissingFieldException

class MainActivity : ComponentActivity() {
    //List of images' ids
    val imageIds = listOf(
        R.drawable.rome,
        R.drawable.ansel,
        R.drawable.kauai,
        R.drawable.despair,
        R.drawable.adventure_beautiful_daylight_325807,
        R.drawable.agriculture_color_cooking_255501,
        R.drawable.ancient_architecture_birds_819806,
        R.drawable.apartments_architecture_bay_632522,
        R.drawable.architecture_autumn_bench_206673,
        R.drawable.arid_barren_black_and_white_150944,
        R.drawable.autumn_autumn_leaves_fall_1563355,
        R.drawable.background_biker_clouds_207171,
        R.drawable.beach_bora_bora_clouds_753626,
        R.drawable.beach_clouds_daytime_994605,
        R.drawable.beach_clouds_dusk_783725,
        R.drawable.caimanphotography3,
        R.drawable.coast_dusk_evening_1048273,
        R.drawable.daylight_forest_glossy_443446,
        R.drawable.eagle_fall_sunrise,
        R.drawable.longue_vue,
        R.drawable.mauisunset,
        R.drawable.mona_big,
        R.drawable.one_wheel,
        R.drawable.rubihorn,
        R.drawable.train_wreck_at_montparnasse_1895,
        R.drawable.yosemite_tree
    )

    @Composable
    //Lazy vertical grid to display the images
    fun CameraRoll(modifier: Modifier = Modifier) {
        val context = LocalContext.current

        //Lazy vertical grid to display images
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(8.dp),
            modifier = modifier.fillMaxSize()
        ) {
            items(imageIds.size) { index ->
                ThumbnailImage(imageIds[index])
            }
        }
    }

    @Composable
    //Thumbnail function to get a thumbnail of each image
    fun ThumbnailImage(imageId: Int) {
        //Declare variables
        val context = LocalContext.current
        var bitmap by remember { mutableStateOf<Bitmap?>(null) }

        //Use a coroutine to get the thumbnail
        LaunchedEffect(imageId) {
            withContext(Dispatchers.IO) {
                try {
                    val original = BitmapFactory.decodeResource(context.resources, imageId)
                    val thumbnail = Bitmap.createScaledBitmap(original, 400, 400, true)
                    bitmap = thumbnail
                } catch (e: Exception) {
                    e.printStackTrace()
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