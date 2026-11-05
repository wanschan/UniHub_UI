package com.example.applicationoneapp.common

import android.content.Context

val Context.app: Context get() = applicationContext

object Pal {
    fun bg(ctx: Context) = Ui.color(ctx, com.example.applicationoneapp.R.color.bg)
    fun ink(ctx: Context) = Ui.color(ctx, com.example.applicationoneapp.R.color.ink)
    fun ink2(ctx: Context) = Ui.color(ctx, com.example.applicationoneapp.R.color.ink_2)
    fun ink3(ctx: Context) = Ui.color(ctx, com.example.applicationoneapp.R.color.ink_3)
    fun ink4(ctx: Context) = Ui.color(ctx, com.example.applicationoneapp.R.color.ink_4)
    fun line(ctx: Context) = Ui.color(ctx, com.example.applicationoneapp.R.color.line)
    fun card(ctx: Context) = Ui.color(ctx, com.example.applicationoneapp.R.color.card)
    fun card2(ctx: Context) = Ui.color(ctx, com.example.applicationoneapp.R.color.card_2)
    fun accent(ctx: Context) = Ui.color(ctx, com.example.applicationoneapp.R.color.accent)
    fun onInk(ctx: Context) = Ui.color(ctx, com.example.applicationoneapp.R.color.on_ink)
    fun hairline(ctx: Context) = Ui.color(ctx, com.example.applicationoneapp.R.color.hairline)
}
