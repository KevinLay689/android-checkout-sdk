package com.paypal.checkoutsamples.token

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.paypal.checkoutsamples.CheckoutConfigHandler
import com.paypal.checkoutsamples.R
import com.paypal.checkoutsamples.sdkhelper.CurrencyCode
import com.paypal.checkoutsamples.sdkhelper.OrderIntent
import com.paypal.checkoutsamples.sdkhelper.UserAction
import com.paypal.checkoutsamples.token.repository.CheckoutApi
import com.paypal.checkoutsamples.token.repository.OrderRepository
import com.paypal.checkoutsamples.token.repository.request.AmountRequest
import com.paypal.checkoutsamples.token.repository.request.ApplicationContextRequest
import com.paypal.checkoutsamples.token.repository.request.OrderRequest
import com.paypal.checkoutsamples.token.repository.request.PurchaseUnitRequest
import com.paypal.pyplcheckout.utils.PayPalCheckoutSdk
import kotlinx.android.synthetic.main.activity_token_quick_start.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import java.io.IOException

class TokenQuickStartActivity : AppCompatActivity() {

    private val tag = this::class.java.toString()

    private val checkoutApi = CheckoutApi()

    private val orderRepository = OrderRepository(checkoutApi)

    private val checkoutSdk: PayPalCheckoutSdk
        get() = PayPalCheckoutSdk.getInstance()

    private val checkoutConfigHandler: CheckoutConfigHandler by lazy {
        CheckoutConfigHandler(supportFragmentManager)
    }

    private val checkoutConfig by lazy {
        checkoutConfigHandler.checkoutConfig
    }

    private val selectedUserAction: UserAction
        get() {
            return when (val selectedId = selectUserAction.checkedRadioButtonId) {
                R.id.userActionOptionContinue -> UserAction.CONTINUE
                R.id.userActionOptionPayNow -> UserAction.PAY_NOW
                else -> {
                    throw IllegalArgumentException(
                        "Expected one of the following ids: ${R.id.userActionOptionContinue}, or " +
                                "${R.id.userActionOptionPayNow} but was $selectedId"
                    )
                }
            }
        }

    private val selectedOrderIntent: OrderIntent
        get() {
            return when (val selectedId = selectOrderIntent.checkedRadioButtonId) {
                R.id.orderIntentOptionAuthorize -> OrderIntent.AUTHORIZE
                R.id.orderIntentOptionCapture -> OrderIntent.CAPTURE
                else -> {
                    throw IllegalArgumentException(
                        "Expected one of the following ids: ${R.id.orderIntentOptionAuthorize}, or " +
                                "${R.id.orderIntentOptionCapture} but was $selectedId"
                    )
                }
            }
        }

    private val selectedCurrencyCode: CurrencyCode
        get() {
            return when (val selectedId = selectCurrencyCode.checkedRadioButtonId) {
                R.id.currencyCodeUsd -> CurrencyCode.USD
                R.id.currencyCodeEur -> CurrencyCode.EUR
                R.id.currencyCodeGbp -> CurrencyCode.GBP
                else -> {
                    throw IllegalArgumentException(
                        "Expected one of the following ids: ${R.id.currencyCodeUsd}, " +
                                "${R.id.currencyCodeEur}, or ${R.id.currencyCodeGbp} but was $selectedId"
                    )
                }
            }
        }

    private val enteredAmount: String
        get() = totalAmountInput.editText!!.text.toString()

    private val uiScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_token_quick_start)

        submitTokenButton.setOnClickListener { button ->
            button.isEnabled = false
            val orderRequest = createOrderRequest()

            uiScope.launch {
                try {
                    val createdOrder = orderRepository.create(orderRequest)
                    startCheckoutWithToken(orderToken = createdOrder.id)
                } catch (ex: IOException) {
                    Log.w(tag, "Attempt to create order failed with the following message: ${ex.message}")
                } finally {
                    button.isEnabled = true
                }
            }
        }
    }

    private fun startCheckoutWithToken(orderToken: String) {
        checkoutSdk.startCheckoutWithToken(
            context = this@TokenQuickStartActivity,
            token = orderToken,
            checkoutConfig = checkoutConfig
        )
    }

    private fun createOrderRequest(): OrderRequest {
        return OrderRequest(
            intent = selectedOrderIntent.name,
            applicationContext = ApplicationContextRequest(
                userAction = selectedUserAction.name
            ),
            purchaseUnits = listOf(
                PurchaseUnitRequest(
                    amount = AmountRequest(
                        value = enteredAmount,
                        currencyCode = selectedCurrencyCode.name
                    )
                )
            )
        ).also { Log.i(tag, "OrderRequest: $it") }
    }

    override fun onStop() {
        super.onStop()
        uiScope.coroutineContext.cancelChildren()
    }

    companion object {
        fun startIntent(context: Context): Intent {
            return Intent(context, TokenQuickStartActivity::class.java)
        }
    }
}