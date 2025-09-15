package com.example.hada_a2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.hada_a2.ui.theme.Hada_A2Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Hada_A2Theme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ShowImages(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun ShowImages(modifier: Modifier = Modifier){

    //Get the ids of all images
    val imageIds = listOf(
        R.drawable.__30,
        R.drawable.rome,
        R.drawable.ansel,
        R.drawable.kauai,
        R.drawable.despair,
        R.drawable._k_wallpaper_adventure_clouds_730981,
        R.drawable._k_wallpaper_cloudiness_clouds_1536809,
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

    Column(modifier = Modifier.fillMaxSize()){
        LazyVerticalGrid(
            GridCells.Fixed(3),
            contentPadding = PaddingValues(8.dp),
            modifier = Modifier.fillMaxSize()
        ){
            items(imageIds.size){ index ->
                val image = painterResource(imageIds[index])
                Image(
                    painter = image,
                    contentDescription = null,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ImagePreview() {
    Hada_A2Theme {
        ShowImages()
    }
}