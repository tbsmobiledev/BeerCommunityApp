package com.beerbuddy.views.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.fragment.app.Fragment
import androidx.cardview.widget.CardView
import android.view.View
import com.beerbuddy.R
import com.beerbuddy.api.ApiProvider
import com.beerbuddy.application.App
import com.beerbuddy.utility.*
import com.beerbuddy.views.fragment.*
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.stocktakeonline.custom_dialog.Dialogs_Custom
import com.utils.BottomNavigationViewHelper
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import listners.Click
import me.leolin.shortcutbadger.ShortcutBadger
import okhttp3.MultipartBody

class MainActivity : BaseOrientationActivity() {


    companion object;
    private val activity1: Activity = this
    private var fragment: Fragment? = null

    private var disposable: Disposable? = null
    private var friend: Disposable? = null
    private var isCheck: String = ""

    /**
     * Bottom navigation
     */
    private val mOnNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        when (item.itemId) {
            R.id.navigation_home -> {
                if (isCheck.equals(item.toString(), true)) {
                    false
                } else {
                    isCheck = item.toString()
                    pushFragment(HomeFragment.newInstance(), "Home Fragment")
                    return@OnNavigationItemSelectedListener true
                }
                //booleanAddress=false
                /*  pushFragment(HomeFragment.newInstance(), "Home Fragment")
                  return@OnNavigationItemSelectedListener true*/
            }
            R.id.navigation_invite -> {
                if (isCheck.equals(item.toString(), true)) {
                    false
                } else {
                    isCheck = item.toString()
                    pushFragment(VisitFragment.newInstance(), "Meet Fragment")
                    return@OnNavigationItemSelectedListener true
                }
            }
            R.id.navigation_friends -> {
                if (isCheck.equals(item.toString(), true)) {
                    false
                } else {
                    isCheck = item.toString()
                    pushFragment(InviteFragment.newInstance(), "Visits Fragment")
                    Session.getInstance(this)?.setFriendReq(0)
                    layout_friend_batch.visibility = View.GONE
                    return@OnNavigationItemSelectedListener true
                }
            }

            R.id.navigation_notifications -> {
                if (isCheck.equals(item.toString(), true)) {
                    false
                } else {
                    isCheck = item.toString()
                    layout_batch.visibility = View.GONE
                    Session.getInstance(this)!!.setBatch(0)
                    ShortcutBadger.removeCount(this)
                    pushFragment(NotificationFragment.newInstance(), "Notification Fragment")
                    return@OnNavigationItemSelectedListener true
                }
            }
            R.id.navigation_more -> {
                if (isCheck.equals(item.toString(), true)) {
                    false
                } else {
                    isCheck = item.toString()
                    pushFragment(MoreListFragment.newInstance(), "More Fragment")
                   // openActivity(MoreListActivity::class.java)
                    return@OnNavigationItemSelectedListener true
                }
            }
        }
        false
    }

    /**
     * Activity life cycle
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        setContentView(R.layout.activity_main)
        val b = isGooglePlayServicesAvailable(this)
        if (b) {
            App.getInstance()?.activity = this
            init()

            disposable = RxBus.subscribe(Consumer { t ->
                run {
                    if (t != 0) {
                        layout_batch.visibility = View.VISIBLE
                        txt_batch.text = (t as Int).toString()
                    } else {
                        txt_batch.text = "0"
                        layout_batch.visibility = View.GONE
                        Session.getInstance(this)?.setBatch(0)
                    }
                    if (fragment is NotificationFragment) {
                        (fragment as NotificationFragment).refreshNewNotification()
                        Session.getInstance(this)?.setBatch(0)
                        ShortcutBadger.removeCount(this)
                        layout_batch.visibility = View.GONE
                    }
                }
            })

            friend = FriendRxBus.subscribe(Consumer { t ->
                run {
                    if (t != 0) {
                        layout_friend_batch.visibility = View.VISIBLE
                        txt_friend_batch.text = (t as Int).toString()
                    } else {
                        txt_friend_batch.text = "0"
                        Session.getInstance(this)?.setFriendReq(0)
                        layout_friend_batch.visibility = View.GONE
                    }
                }
            })

            if (Session.getInstance(this)!!.getBatch() != 0) {
                txt_batch.text = Session.getInstance(this)?.getBatch()?.toString()
            } else {
                txt_batch.text = Session.getInstance(this)?.getBatch()?.toString()
                layout_batch.visibility = View.GONE
            }
            if (Session.getInstance(this)!!.getFriendReq() != 0) {
                txt_friend_batch.text = Session.getInstance(this)?.getFriendReq()?.toString()
            } else {
                txt_friend_batch.text = "0"
                Session.getInstance(this)?.setFriendReq(0)
                layout_friend_batch.visibility = View.GONE
            }
        } else {
            Dialogs_Custom.singleButtonDialog(activity1,activity1.resources.getString(R.string.app_name_alert),
                    activity1.resources.getString(R.string.google_play_service),
                    activity1.resources.getString(R.string.txt_Ok),object : Click {
                override fun onclick(position: Int, `object`: Any, text: String) {
                    if (text.equals(resources.getString(R.string.ok), true)) {
                    } else {
                    }
                }
            })
        }
    }

    private fun isGooglePlayServicesAvailable(activity: Activity): Boolean {
        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val status = googleApiAvailability.isGooglePlayServicesAvailable(activity)
        if (status != ConnectionResult.SUCCESS) {
            if (googleApiAvailability.isUserResolvableError(status)) {
                googleApiAvailability.getErrorDialog(activity, status, 2404).show()
            }
            return false
        }
        return true
    }

    /**
     * Activity life cycle
     */
    override fun onDestroy() {
        super.onDestroy()
        disposable?.dispose()
        friend?.dispose()
    }

    /**
     * Method to push any fragment into given id  @param fragment An instance of Fragment to show into the given id
     */
    private fun pushFragment(fragment: Fragment?, s: String) {
        this.fragment = fragment
        if (fragment == null)
            return
        val fragmentManager = supportFragmentManager
        if (fragmentManager != null) {
            val ft = fragmentManager.beginTransaction()
            if (ft != null) {
                ft.replace(R.id.container, fragment, s)
                ft.commitAllowingStateLoss()
            }
        }
    }

    /**
     * Variable initialization
     */
    private fun init() {
        try {
            navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)
            navigation.itemIconTintList = null
            BottomNavigationViewHelper.disableShiftMode(navigation, this)
            if (intent.getBooleanExtra("isNotification", false)) {
                checkNotificationOrForPopUp(intent)
                navigation.selectedItemId = R.id.navigation_notifications
            } else
                navigation.selectedItemId = R.id.navigation_home
        } catch (e: Exception) {
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent?.getBooleanExtra("isNotification", false) == true) {
            checkNotificationOrForPopUp(intent)
            navigation.selectedItemId = R.id.navigation_notifications

        } else
            navigation.selectedItemId = R.id.navigation_home
    }

    /**
     * req permission for granted
     */
    override fun onPermissionGranted(PermissionCode: Int) {
        super.onPermissionGranted(PermissionCode)
        if (Constant.LOCATION_PERMISSION == PermissionCode) {
            if (fragment is HomeFragment) {
                (fragment as HomeFragment).onPermissionGranted(Constant.LOCATION_PERMISSION)
            }
        } else if (Constant.CONTACT_LISTING_PERMISSION == PermissionCode) {
            if (fragment is InviteFragment) {
                (fragment as InviteFragment).onPermissionGranted(Constant.LOCATION_PERMISSION)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        data?.let {
            if (requestCode == Constant.REQUEST_CODE_AUTOCOMPLETE) {
                if (fragment is HomeFragment) {
                    (fragment as HomeFragment).onActivityResult(requestCode, resultCode, data)
                }
            }

            if (requestCode == Constant.SEARCH_DETAIL) {
                if (fragment is HomeFragment) {
                    (fragment as HomeFragment).onActivityResult(requestCode, resultCode, data)
                }
            } else if (requestCode == Constant.BAR_DETAIL) {
                if (resultCode == Activity.RESULT_OK) {
                    when (data.getIntExtra("no_friend", 0)) {
                        BarDetailActivity.NoUserFriends.No.code -> {
                            navigation.selectedItemId = R.id.navigation_friends
                        }
                        BarDetailActivity.NoUserFriends.Home.code -> {
                            navigation.selectedItemId = R.id.navigation_home
                        }
                        else -> {
                            navigation.selectedItemId = R.id.navigation_invite
                        }
                    }
                }
            } else if (requestCode == Constant.CHECKIN) {
                if (resultCode == Activity.RESULT_OK) {
                    navigation.selectedItemId = R.id.navigation_home
                }
            } else if (requestCode == Constant.CONTACT_SYNC) {
                (fragment as InviteFragment).onActivityResult(requestCode, resultCode, data)
            } else if (requestCode == Constant.EVENT_DATA) {
                (fragment as VisitFragment).onActivityResult(requestCode, resultCode, data)
            }

            if (requestCode == Constant.MORE_PAGE) {
                if (fragment is HomeFragment) {
                    (fragment as HomeFragment).onActivityResult(requestCode, resultCode, data)
                }
            }
        }
    }

    /**
     * Notification Popup for checkout
     */
    private fun checkNotificationOrForPopUp(intent: Intent?) {

        if (intent != null && intent.hasExtra("notification_type")) {
            if (intent.hasExtra("shop_id") && intent.getIntExtra("notification_type", 0) == 4) {

                val message: String? = intent.getStringExtra("message")
                val mTitle: String? = intent.getStringExtra("title")
                showCheckoutPopup(intent.getStringExtra("shop_id"), message, mTitle)
            }
        }
    }

    private fun showCheckoutPopup(shopId: String, message: String?, mTitle: String?) {
        val dialog = Utility.showCheckoutDialog(this@MainActivity, "" + message, "" + mTitle)
        dialog?.findViewById<CardView>(R.id.card_lyt_continue)?.setOnClickListener {
            dialog.dismiss()
            checkOut(shopId)
        }
        dialog?.findViewById<CardView>(R.id.card_lyt_cancel)?.setOnClickListener { dialog.dismiss() }
    }

    /**
     * Check  out API
     */
    @SuppressLint("CheckResult")
    private fun checkOut(beerShopId: String) {
        val imagesList = java.util.ArrayList<MultipartBody.Part>()
        ApiProvider.checkOut(Constant.checkOut, this)
                .checkOut(beerShopId, "false", imagesList)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({

                }, {

                })
    }

    /**
     * Back to home otherwise close
     */
    override fun onBackPressed() {
        if (navigation.selectedItemId != R.id.navigation_home) {
            navigation.selectedItemId = R.id.navigation_home
        } else {
            finish()
        }
    }
}
