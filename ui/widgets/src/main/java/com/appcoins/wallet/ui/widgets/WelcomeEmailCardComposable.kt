package com.appcoins.wallet.ui.widgets

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appcoins.wallet.ui.common.theme.WalletColors
import com.appcoins.wallet.ui.widgets.component.Animation
import com.appcoins.wallet.ui.widgets.component.ButtonType
import com.appcoins.wallet.ui.widgets.component.ButtonWithText
import com.appcoins.wallet.ui.widgets.component.WalletTextField

@Composable
fun WelcomeEmailCard(
  email: MutableState<String>,
  onSendClick: () -> Unit,
  onCloseClick: () -> Unit,
  isError: Boolean = false,
  errorText: String,
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp)
      .padding(bottom = 8.dp, top = 16.dp)
      .background(WalletColors.styleguide_blue_secondary, shape = RoundedCornerShape(16.dp))
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(end = 8.dp, top = 8.dp),
      horizontalAlignment = Alignment.End
    ) {
      Icon(
        painter = painterResource(id = R.drawable.ic_close_rounded),
        contentDescription = "Close",
        tint = Color.Unspecified,
        modifier = Modifier
          .size(18.dp)
          .clickable(onClick = onCloseClick),
      )
    }
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(end = 16.dp, bottom = 8.dp, start = 16.dp, top = 0.dp)
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Animation(modifier = Modifier.size(30.dp), animationRes = R.raw.bonus_gift_animation)
        Text(
          text = stringResource(id = R.string.mail_list_card_title),
          color = Color.White,
          fontSize = 16.sp,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.padding(start = 8.dp)
        )
      }
      Text(
        text = stringResource(id = R.string.mail_list_card_body),
        color = Color.White,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 10.dp, end = 16.dp, start = 4.dp)
      )
      Spacer(modifier = Modifier.height(19.dp))
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 2.dp)
          .height(52.dp)
      ) {
        Box(
          modifier = Modifier
            .background(color = WalletColors.styleguide_blue, shape = RoundedCornerShape(24.dp))
            .border(
              border = if (isError) BorderStroke(
                width = 1.dp,
                color = WalletColors.styleguide_red
              ) else BorderStroke(width = 0.dp, color = Color.Transparent),
              shape = RoundedCornerShape(24.dp)
            )
        ) {
          WalletTextField(
            modifier = Modifier.fillMaxWidth(),
            email.value,
            stringResource(R.string.email_here_field),
            backgroundColor = WalletColors.styleguide_blue,
            keyboardType = KeyboardType.Email,
            roundedCornerShape = RoundedCornerShape(24.dp)
          ) { newEmail ->
            email.value = newEmail
          }
          ButtonWithText(
            label = stringResource(id = com.appcoins.wallet.ui.common.R.string.send_button),
            onClick = { onSendClick() },
            backgroundColor = WalletColors.styleguide_pink,
            labelColor = WalletColors.styleguide_light_grey,
            buttonType = ButtonType.DEFAULT,
            enabled = true,
            modifier = Modifier
              .align(Alignment.CenterEnd)
              .padding(end = 6.dp)
          )
        }
      }
      Text(
        text = if (isError) errorText else "",
        color = WalletColors.styleguide_red,
        fontSize = 11.sp,
        modifier = Modifier.padding(start = 16.dp)
      )
    }
  }
}

@Preview
@Composable
fun PreviewHomeEmailComposable() {
  val email = remember { mutableStateOf("") }
  WelcomeEmailCard(email, {}, {}, false, stringResource(R.string.error_general))
}

@Preview
@Composable
fun PreviewHomeEmailErrorComposable() {
  val email = remember { mutableStateOf("") }
  WelcomeEmailCard(email, {}, {}, true, stringResource(R.string.error_general))
}