package com.example.appjardin.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.appjardin.R
import com.example.appjardin.model.Pet
import com.example.appjardin.model.PetMood
import com.example.appjardin.ui.theme.ColorVerdeAlegre
import com.example.appjardin.ui.theme.DarkText

@Composable
fun PetCongratulationsDialog(
    pet: Pet,
    effectivePetName: String,
    activeColor: Color = ColorVerdeAlegre,
    onEquip: () -> Unit,
    onDismiss: () -> Unit
) {
    val descriptionRes = when (pet) {
        Pet.LARVA -> R.string.pet_desc_larva
        Pet.GUSANO -> R.string.pet_desc_gusano
        Pet.HORMIGA -> R.string.pet_desc_hormiga
        Pet.CHANCHITO -> R.string.pet_desc_chanchito
        Pet.ABEJA -> R.string.pet_desc_abeja
        Pet.REYGEKO -> R.string.pet_desc_reygeko
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.congratulations_title),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorVerdeAlegre,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                Image(
                    painter = painterResource(id = pet.getDrawable(PetMood.FELIZ)),
                    contentDescription = effectivePetName,
                    modifier = Modifier.size(110.dp),
                    contentScale = ContentScale.Fit
                )
            }
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = stringResource(R.string.congratulations_text, effectivePetName),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkText,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = stringResource(descriptionRes),
                    fontSize = 13.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onEquip,
                colors = ButtonDefaults.buttonColors(containerColor = activeColor),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .height(48.dp)
                    .widthIn(min = 100.dp)
            ) {
                Text(
                    text = stringResource(R.string.btn_equip),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color.Gray),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Gray),
                modifier = Modifier
                    .height(48.dp)
                    .widthIn(min = 90.dp)
            ) {
                Text(
                    text = stringResource(R.string.close),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(20.dp)
    )
}

@Preview(showBackground = true)
@Composable
fun PetCongratulationsDialogPreview() {
    MaterialTheme {
        PetCongratulationsDialog(
            pet = Pet.ABEJA,
            effectivePetName = "Miel",
            activeColor = ColorVerdeAlegre,
            onEquip = {},
            onDismiss = {}
        )
    }
}
