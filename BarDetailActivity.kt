package com.beerbuddy.views.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.TextUtils
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.beerbuddy.R
import com.beerbuddy.api.ApiProvider
import com.beerbuddy.utility.Constant
import com.beerbuddy.utility.Constant.CONTACT_LISTING
import com.beerbuddy.utility.Constant.FEEDBACK
import com.beerbuddy.utility.Constant.GET_CAMERA_PERMISSION
import com.beerbuddy.utility.Constant.SELECT_IMAGE
import com.beerbuddy.utility.Constant.TAKE_PHOTO_CODE
import com.beerbuddy.utility.MultiPart
import com.beerbuddy.utility.Session
import com.beerbuddy.utility.Utility.saveBitmapToFile
import com.beerbuddy.utility.Utility.showDialogValidation
import com.beerbuddy.views.adapter.CheckedInBuddyAdapter
import com.beerbuddy.views.adapter.ImageAdapter
import com.beerbuddy.views.adapter.InvitedBuddiesAdapter
import com.beerbuddy.views.enum_p.ApiResponseCode
import com.beerbuddy.views.enum_p.FriendStatus
import com.beerbuddy.views.model.*
import com.stocktakeonline.custom_dialog.Dialogs_Custom
import com.utils.KeyboardUtils
import com.utils.UserPicture
import com.utils.Utility
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.content_bar_detail.*
import kotlinx.android.synthetic.main.detail_action_bar.*
import listners.Click
import okhttp3.MultipartBody
import org.json.JSONObject
import java.io.File
import java.io.IOException

class BarDetailActivity : BaseOrientationActivity() {

    private val activity: Activity = this

    var isFavorate: Boolean = false

    private val check = arrayOf<CharSequence>("Take Photo", "Choose from Gallery", "Cancel")

    private val imageList: MutableList<CheckOutRequest> = mutableListOf()

    private var results: Result? = null

    private var mEventId: Int? = 0

    private var isIndividual: Boolean = false

    private var contactData: Beershopdetail? = null
    private var invitedBuddiesList: InvitedBuddiesList? = null

    private var mTotalCommentCount: String? = "0"

    enum class NoUserFriends(var code: Int) {
        No(1),
        Home(2),
        Visit(3)
    }

    @SuppressLint("WrongConstant")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)
        setContentView(R.layout.activity_bar_detail)


        contactData = intent?.getParcelableExtra("contact")
        results = intent?.getParcelableExtra<Result>("data")
        isIndividual = intent.getBooleanExtra("isIndividual", false)

        if (intent.getBooleanExtra("isMap", false)) { // if coming from map

            val gradient = GradientDrawable()
            gradient.setStroke(2, ContextCompat.getColor(this, R.color.color_707070))
            gradient.cornerRadius = 20f
            edt_check_in.setTextColor(ContextCompat.getColor(this, R.color.color_000000))
            edt_check_in.background = gradient

            buddy_heading.text = getString(R.string.txt_added_buddies)
            add_buddy_layout.visibility = View.GONE
            if (contactData?.user_contact_list?.size as Int > 0) {
                check_in_buddy_layout.visibility = View.VISIBLE
                list_check_in_buddies?.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
                list_check_in_buddies?.adapter = CheckedInBuddyAdapter(this, contactData?.user_contact_list, object : CheckedInBuddyAdapter.UserClick {
                    override fun otherUser(item: UserConnectedItem?) {
                        activity?.let { it1 ->

                            item?.id?.let { it2 ->

                                if (Session.getInstance(activity)?.getUserId()?.equals(it2.toString())!!) {
                                    val intent = Intent(activity, ProfileActivity::class.java)
                                    startActivity(intent)
                                } else {
                                    (activity as BaseActivity).showProgressDialog()
                                    ApiProvider.otherUserProfile(Constant.socialMobile, it1)
                                            .getOtherUserProfile(it2).subscribeOn(Schedulers.io())
                                            .observeOn(AndroidSchedulers.mainThread())
                                            .subscribe({
                                                (activity as BaseActivity).hideProgressDialog()
                                                if (it.status == ApiResponseCode.success.code) {
                                                    val intent = Intent(activity, OtherUserProfileActivity::class.java)
                                                    intent.putExtra("data", it)
                                                    startActivity(intent)
                                                } else {
                                                    activity.hideProgressDialog()
                                                    it?.message?.let { it3 -> 
                                                        Dialogs_Custom.singleButtonDialog(activity, activity.resources.getString(R.string.app_name_alert),
                                                                it3,
                                                                activity.resources.getString(R.string.txt_Ok), object : Click {
                                                            override fun onclick(position: Int, `object`: Any, text: String) {
                                                                if (text.equals(resources.getString(R.string.ok), true)) {
                                                                } else {
                                                                }
                                                            }
                                                        })
                                                    }
                                                }
                                            }, {
                                                activity.errorHandler(it, activity)
                                            })
                                }
                            }
                        }

                    }

                })



            }
            if (isIndividual) { // if it is individual

                if (contactData?.user_check_in == true) { // if already logged in show check out
                    btn_check_in.text = getString(R.string.check_out)
                   // btn_invite.visibility = View.GONE

                } else {  // else check in
                    btn_check_in.text = getString(R.string.btn_check_in)
                    btn_invite.visibility = View.VISIBLE
                }
            } else { // if user wants to invite buddies
                if (contactData?.user_check_in == true) {
                    // if already loged in show check out
                    edt_check_in.visibility = View.GONE
                    btn_check_in.text = getString(R.string.check_out)
                   // btn_invite.visibility = View.GONE

                } else {
                    // else show send invite text
                    edt_check_in.visibility = View.VISIBLE
                    btn_invite.visibility = View.VISIBLE

                }
            }
        } else {
            // if not coming from the map
            mRelativeLayout?.visibility = View.VISIBLE
            val gradient = GradientDrawable()
            gradient.setStroke(2, ContextCompat.getColor(this, R.color.color_B9B9B9))
            gradient.cornerRadius = 20f
            edt_check_in.setTextColor(ContextCompat.getColor(this, R.color.color_000000))
            edt_check_in.background = gradient

            if (!isIndividual) { // if individual
                invitedBuddiesList = intent.getParcelableExtra("invited")
                list_added_buddies.layoutManager = LinearLayoutManager(this, LinearLayout.HORIZONTAL, false)
                list_added_buddies.adapter = invitedBuddiesList?.data?.toMutableList()?.let {
                    if (it.size > 0) {
                        setTotalCount(it.get(0)?.comment_count)
                    }
                    InvitedBuddiesAdapter(this, it, object : InvitedBuddiesAdapter.UserClick {
                        override fun otherUser(item: InvitedBuddiesData?) {
                            activity?.let { it1 ->

                                item?.id?.let { it2 ->

                                    if (Session.getInstance(activity)?.getUserId()?.equals(it2.toString())!!) {
                                        val intent = Intent(activity, ProfileActivity::class.java)
                                        startActivity(intent)
                                    } else {
                                        (activity as BaseActivity).showProgressDialog()
                                        ApiProvider.otherUserProfile(Constant.socialMobile, it1)
                                                .getOtherUserProfile(it2).subscribeOn(Schedulers.io())
                                                .observeOn(AndroidSchedulers.mainThread())
                                                .subscribe({
                                                    activity.hideProgressDialog()
                                                    if (it.status == ApiResponseCode.success.code) {
                                                        val intent = Intent(activity, OtherUserProfileActivity::class.java)
                                                        intent.putExtra("data", it)
                                                        startActivity(intent)
                                                    } else {
                                                        (activity as BaseActivity).hideProgressDialog()
                                                        it?.message?.let { it3 -> 
                                                            Dialogs_Custom.singleButtonDialog(activity, activity.resources.getString(R.string.app_name_alert),
                                                                    it3,
                                                                    activity.resources.getString(R.string.txt_Ok), object : Click {
                                                                override fun onclick(position: Int, `object`: Any, text: String) {
                                                                    if (text.equals(resources.getString(R.string.ok), true)) {
                                                                    } else {
                                                                    }
                                                                }
                                                            })
                                                        }
                                                    }
                                                }, {
                                                    (activity as BaseActivity).errorHandler(it, activity)
                                                })
                                    }
                                }
                            }

                        }

                    })
                }

                if (intent != null && intent?.hasExtra("event_id")!!) {
                    mEventId = intent?.getIntExtra("event_id", 0)
                }

                add_buddy_layout.visibility = View.VISIBLE
                buddy_heading.text = getString(R.string.invited_buddies)
                if (contactData?.user_check_in == true) {
                    btn_check_in.text = getString(R.string.check_out)
                   // btn_invite.visibility = View.GONE
                } else {
                    btn_check_in.text = getString(R.string.btn_check_in)
                   // btn_invite.visibility = View.GONE
                }
                if (!TextUtils.isEmpty(intent?.extras?.getString("message"))) {

                    edt_check_in?.isEnabled = false
                    edt_check_in?.setText(intent?.extras?.getString("message"))
                } else {
                    edt_check_in.visibility = View.VISIBLE
                    edt_check_in?.isEnabled = false
                }
            } else {
                edt_check_in?.isEnabled = false
                edt_check_in.visibility = View.VISIBLE
                add_buddy_layout.visibility = View.GONE
                mRelativeLayout?.visibility = View.INVISIBLE
                if (contactData?.user_check_in == true) {
                    btn_check_in.text = getString(R.string.check_out)
                    //btn_invite.visibility = View.GONE
                } else {
                    btn_check_in.text = getString(R.string.btn_check_in)
                    //btn_invite.visibility = View.GONE
                }
            }
        }

        upload_layout.visibility = View.GONE
        initialize()
    }

    // initialize
    private fun initialize() {
        top_layout_click.viewTreeObserver.addOnGlobalLayoutListener {
            val r = Rect()
            top_layout_click.getWindowVisibleDisplayFrame(r)
            val screenHeight = top_layout_click.rootView.height
            val keypadHeight = screenHeight - r.bottom

            if (keypadHeight > screenHeight * 0.15) { // 0.15 ratio is perhaps enough to determine keypad height.
                // keyboard is opened
                top_layout_click.setOnClickListener { KeyboardUtils.hideKeyboard(this) }
            } else {
                // keyboard is closed
                top_layout_click.setOnClickListener(null)
            }
        }

        txt_all_reviews.paintFlags = txt_all_reviews.paintFlags or Paint.UNDERLINE_TEXT_FLAG
        txt_all_reviews.text = getString(R.string.txt_see_all_review)

        results?.let {

            library_normal_ratingbar.rating = contactData?.rating ?: 0f

            bar_address.paintFlags = bar_address.paintFlags or Paint.UNDERLINE_TEXT_FLAG

            bar_address.text = when (!TextUtils.isEmpty(results?.vicinity)) {
                true -> "" + results?.vicinity
                false -> "" + results?.formatted_address
            }

            bar_address.setOnClickListener {
                if (isGoogleMapsInstalled()) {
                    // Create a Uri from an intent string. Use the result to create an Intent.
                    val gmmIntentUri = Uri.parse("google.navigation:q=" + results?.geometry?.location?.lat
                            + "," + results?.geometry?.location?.lng + "")
                    // Create an Intent from gmmIntentUri. Set the action to ACTION_VIEW
                    val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                    // Make the Intent explicit by setting the Google Maps package
                    mapIntent.`package` = "com.google.android.apps.maps"

                    // Attempt to start an activity that can handle the Intent
                    startActivity(mapIntent)
                } else {
                    Dialogs_Custom.twoButtonDialog(activity, activity.resources.getString(R.string.alert_google_maps),
                            activity.resources.getString(R.string.alert_google_map_msg),
                            activity.resources.getString(R.string.cancel),
                            activity.resources.getString(R.string.ok), object : Click {
                        override fun onclick(position1: Int, `object`: Any, text: String) {
                            if (text.equals(activity.resources.getString(R.string.ok), true)) {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.google.android.apps.maps"))
                                startActivity(intent)
                            } else {
                            }

                        }
                    })
                }
            }


            ll_location.setOnClickListener {
                if (isGoogleMapsInstalled()) {
                    // Create a Uri from an intent string. Use the result to create an Intent.
                    val gmmIntentUri = Uri.parse("google.navigation:q=" + results?.geometry?.location?.lat
                            + "," + results?.geometry?.location?.lng + "")
                    // Create an Intent from gmmIntentUri. Set the action to ACTION_VIEW
                    val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                    // Make the Intent explicit by setting the Google Maps package
                    mapIntent.`package` = "com.google.android.apps.maps"

                    // Attempt to start an activity that can handle the Intent
                    startActivity(mapIntent)
                } else {
                    Dialogs_Custom.twoButtonDialog(activity, activity.resources.getString(R.string.alert_google_maps),
                            activity.resources.getString(R.string.alert_google_map_msg),
                            activity.resources.getString(R.string.cancel),
                            activity.resources.getString(R.string.ok), object : Click {
                        override fun onclick(position1: Int, `object`: Any, text: String) {
                            if (text.equals(activity.resources.getString(R.string.ok), true)) {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.google.android.apps.maps"))
                                startActivity(intent)
                            } else {
                            }

                        }
                    })
                }
            }

            beer_name.text = results?.name
        }

        txt_all_reviews.setOnClickListener {
            val intent = Intent(this, UserRatingReviewActivity::class.java)
            intent.putExtra("id", contactData?.beer_shop_id)
            startActivity(intent)
        }

        btn_back.setOnClickListener {
            val intent = Intent()
            setResult(Activity.RESULT_CANCELED, intent)
            finish()
        }

        mLinearRating.setOnClickListener {
            val bundle = Bundle()
            val intent = Intent(this, FeedBackActivity::class.java)
            bundle.putString("name", contactData?.beer_shop_name)
            bundle.putString("data", contactData?.beer_shop_id?.toString())
            startActivityForResult(intent.putExtras(bundle), FEEDBACK)
        }

        btn_invite?.setOnClickListener {
            if (com.beerbuddy.utility.Utility.isOnline(this)) {

                var intent = Intent(this, ContactListing::class.java)
                intent.putExtra("tournamentBean", invitedBuddiesList)
                startActivityForResult(intent, CONTACT_LISTING)

            } else {
                Dialogs_Custom.singleButtonDialog(activity, activity.resources.getString(R.string.app_name_alert),
                        activity.resources.getString(R.string.alert_internet),
                        activity.resources.getString(R.string.txt_Ok), object : Click {
                    override fun onclick(position: Int, `object`: Any, text: String) {
                        if (text.equals(resources.getString(R.string.ok), true)) {
                        } else {
                        }
                    }
                })

            }
        }

        btn_check_in.setOnClickListener {
            if (btn_check_in.text.toString() == getString(R.string.check_out)) {
                checkOut(contactData?.beer_shop_id?.toString()!!, contactData?.beer_shop_name, true)
            } else {
                checkInApi()
            }
        }

        if (contactData?.favorite == true) {
            isFavorate = true
            favorite_icon.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.favorite_like_icon))
        } else {
            isFavorate = false
            favorite_icon.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.favorite_unlike_icon))
        }
        favorite_icon_layout.setOnClickListener {
            if (!isFavorate) {
                isFavorate = true
                makeFavorite(1)
            } else {
                isFavorate = false
                makeFavorite(0)

            }
        }

        list_uploaded_image.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        list_uploaded_image.adapter = ImageAdapter(this, imageList
                , object : ImageAdapter.IclickEvent {
            override fun clickListener(pos: Int) {
                if (pos == 0) {
                    upload_layout.visibility = View.GONE
                }
            }
        })

        /* reply click button */
        mTxtOpenChatActivity?.setOnClickListener {
            val bundle = Bundle()
            bundle.putInt("event_id", mEventId!!)
            bundle.putString("total_comment_count", mTotalCommentCount)
            startActivityForResult(Intent(this, EventChatActivity::class.java).putExtras(bundle), Constant.EVENT_CHAT)
        }

    }

    // Method for API sending invites to the buddies
    @SuppressLint("CheckResult")
    private fun sendInvite(finalList: MutableList<UserContactInfo?>) {

        showProgressDialog()
        val listOfInvitedBuddies: MutableList<String> = mutableListOf()

        for (mObject in finalList) {
            mObject?.message = "Hide"
            listOfInvitedBuddies.add(mObject?.email ?: "")
        }

        val sendInviteRequest = SendInviteRequest()

        sendInviteRequest.invitee = listOfInvitedBuddies
        sendInviteRequest.beer_shop = contactData?.beer_shop_id?.toString()
        sendInviteRequest.location = results?.formatted_address
        sendInviteRequest.message = if (TextUtils.isEmpty(edt_check_in?.text?.toString())) getString(R.string.hint_check_in_status) else edt_check_in?.text?.toString()
        sendInviteRequest.status = FriendStatus.PENDING.status

        ApiProvider.sendInvite(Constant.sendInvite, this)
                .sendInvite(sendInviteRequest)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    hideProgressDialog()
                    val j = JSONObject(it.toString())
                    if (ApiResponseCode.success.code == j.optInt("status")) {

                        val intent = Intent()
                        intent.putExtra("no_friend", NoUserFriends.Visit.code)
                        setResult(Activity.RESULT_OK, intent)
                        finish()

                    } else {
                        Dialogs_Custom.singleButtonDialog(activity, activity.resources.getString(R.string.app_name_alert),
                                j.optString("message"),
                                activity.resources.getString(R.string.txt_Ok), object : Click {
                            override fun onclick(position: Int, `object`: Any, text: String) {
                                if (text.equals(resources.getString(R.string.ok), true)) {
                                } else {
                                }
                            }
                        })

                    }
                }, {
                    hideProgressDialog()
                    errorHandler(it, this)
                })
    }

    // making the bar as favorites
    @SuppressLint("CheckResult")
    private fun makeFavorite(status: Int) {
        showProgressDialog()
        val favourite = Favourite()
        favourite.beer_id = contactData?.beer_shop_id ?: 0
        favourite.latitude = results?.geometry?.location?.lat ?: 0.0
        favourite.longitude = results?.geometry?.location?.lng ?: 0.0
        favourite.status = status
        ApiProvider.setFavourite(Constant.favourite, this)
                .setFavouriteApi(favourite = favourite)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    hideProgressDialog()
                    val j = JSONObject(it.toString())
                    if (ApiResponseCode.success.code == j.getInt("status")) {
                        if (status == 1) {
                            favorite_icon.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.favorite_like_icon))
                        } else {
                            favorite_icon.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.favorite_unlike_icon))
                        }
                    }
                }, {
                    hideProgressDialog()
                    errorHandler(it, this)

                })
    }

    // Check in API
    @SuppressLint("CheckResult")
    private fun checkInApi() {
        showProgressDialog()
        val check = CheckIn()
        check.beer_id = contactData?.beer_shop_id
        check.checkin = true

        ApiProvider.checkIn(Constant.checkIn, this)
                .checkIn(check)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    hideProgressDialog()
                    if (it.status == ApiResponseCode.success.code) {
                        if (it?.data?.beer_id == 0 && TextUtils.isEmpty(it.data?.beer_name)) {
                            val dialog = showDialogValidation(this, getString(R.string.msg_check))
                            dialog.findViewById<androidx.cardview.widget.CardView>(R.id.card_lyt_cancel).setOnClickListener { dialog.dismiss() }
                            dialog.findViewById<androidx.cardview.widget.CardView>(R.id.card_lyt_continue).setOnClickListener {
                                dialog.dismiss()
                                btn_check_in.text = getString(R.string.check_out)
                                //btn_invite.visibility = View.GONE
                            }
                        } else {

                            val dialog = it?.message?.let { it1 -> showDialogValidation(this, it1) }
                            dialog?.findViewById<LinearLayout>(R.id.cancel_layout)?.visibility = View.VISIBLE
                            dialog?.findViewById<androidx.cardview.widget.CardView>(R.id.card_lyt_cancel)?.setOnClickListener { dialog.dismiss() }
                            dialog?.findViewById<androidx.cardview.widget.CardView>(R.id.card_lyt_continue)?.setOnClickListener(object : View.OnClickListener {
                                override fun onClick(p0: View?) {
                                    dialog.dismiss()
                                    checkOut(it.data?.beer_id?.toString()
                                            ?: "", it.data?.beer_name, false)
                                }
                            })
                        }
                    }
                }, {
                    hideProgressDialog()
                    errorHandler(it, this)
                })
    }

    // Check out API
    @SuppressLint("CheckResult")
    private fun checkOut(beerShopId: String, beer_shop_name: String?, b: Boolean) {
        showProgressDialog()
        val imagesList = java.util.ArrayList<MultipartBody.Part>()
        imageList.indices
                .map { File(imageList[it].path) }
                .mapTo(imagesList) { Utility.prepareFilePart("images", Uri.fromFile(it), this) }
        ApiProvider.checkOut(Constant.checkOut, this)
                .checkOut(beerShopId, "false", imagesList)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    hideProgressDialog()
                    val j = JSONObject(it.toString())
                    if (ApiResponseCode.success.code == j.getInt("status")) {
                        btn_check_in.text = getString(R.string.btn_check_in)
                        btn_invite.visibility = View.VISIBLE
                        if (b) {
                            val dialog = showDialogValidation(this, String.format(getString(R.string.check_out_successfully)
                                    , beer_shop_name))
//
                            dialog.findViewById<LinearLayout>(R.id.cancel_layout).visibility = View.GONE
                            dialog.findViewById<androidx.cardview.widget.CardView>(R.id.card_lyt_continue).setOnClickListener {
                                dialog.dismiss()
                                val intent = Intent()
                                intent.putExtra("no_friend", NoUserFriends.Home.code)
                                setResult(Activity.RESULT_OK, intent)
                                finish()
                            }
                            dialog.findViewById<androidx.cardview.widget.CardView>(R.id.card_lyt_cancel).setOnClickListener {
                                dialog.dismiss()
                                val intent = Intent()
                                intent.putExtra("no_friend", NoUserFriends.Home.code)
                                setResult(Activity.RESULT_OK, intent)
                                finish()
                            }
                        } else {
                            checkInApi()
                        }
                    } else {
                        Dialogs_Custom.singleButtonDialog(activity, activity.resources.getString(R.string.app_name_alert),
                                j.optString("message"),
                                activity.resources.getString(R.string.txt_Ok), object : Click {
                            override fun onclick(position: Int, `object`: Any, text: String) {
                                if (text.equals(resources.getString(R.string.ok), true)) {
                                } else {
                                }
                            }
                        })

                    }
                }, {
                    hideProgressDialog()
                    errorHandler(it, this)
                })
    }

    override fun onPermissionGranted(PermissionCode: Int) {
        super.onPermissionGranted(PermissionCode)

        when (PermissionCode) {
            GET_CAMERA_PERMISSION -> {
                if (imageList.size < 4)
                    choosePhoto()
                else {
                    val dialog = showDialogValidation(this, getString(R.string.show_number_of_image))
                    dialog.findViewById<LinearLayout>(R.id.cancel_layout)?.visibility = View.GONE
                    dialog.findViewById<androidx.cardview.widget.CardView>(R.id.card_lyt_cancel)?.setOnClickListener { dialog.dismiss() }
                    dialog.findViewById<androidx.cardview.widget.CardView>(R.id.card_lyt_continue)?.setOnClickListener { dialog.dismiss() }
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == CONTACT_LISTING) {
                val i = data?.getIntExtra("click", 1)
                when (i) {
                    ContactListing.AddBuddy.ADD.click -> {

                        val finalList: MutableList<UserContactInfo?> = data.getParcelableArrayListExtra("data")!!
                        sendInvitation(finalList)
                    }
                    ContactListing.AddBuddy.NO_FRIEND.click -> {
                        val intent = Intent()
                        intent.putExtra("no_friend", NoUserFriends.No.code)
                        intent.putExtra("is", true)
                        setResult(Activity.RESULT_OK, intent)
                        finish()
                    }
                }

            } else if (requestCode == FEEDBACK) {
                val checkOut = data?.getBooleanExtra("check_out", false)
                when (checkOut) {
                    true -> {
                       /* val intent = Intent()
                        intent.putExtra("no_friend", NoUserFriends.Home.code)
                        setResult(Activity.RESULT_OK, intent)
                        finish()*/
                        onBackPressed()
                    }
                    false -> {

                    }
                }

            } else if (requestCode == Constant.EVENT_CHAT) {
                val mTotalCount = data?.getStringExtra("total_comment_count")
                setTotalCount(mTotalCount)
            } else if (requestCode == SELECT_IMAGE) {
                if (data != null) try {
                    Thread {
                        val path = getAbsoluteFilePath(data, this)
                        var b = File(path)
                        val fileSizeInBytes = b.length()
                        val fileSizeInKB = fileSizeInBytes.div(1024)
                        val fileSizeInMB = fileSizeInKB.div(1024)
                        if (fileSizeInMB > 1) {
                            val s = MultiPart.compressImage(Uri.fromFile(b).toString(), this)
                            b = File(s)
                        }
                        val filePath = b.path
                        val bitmap = BitmapFactory.decodeFile(filePath)
                        val item = CheckOutRequest()
                        item.bitmap = bitmap
                        item.path = filePath
                        imageList.add(item)
                        runOnUiThread {
                            upload_layout.visibility = View.VISIBLE
                            list_uploaded_image.setItemViewCacheSize(imageList.size.minus(1))
                            (list_uploaded_image.adapter as ImageAdapter).refreshList(imageList)
                        }
                    }.start()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            } else if (requestCode == TAKE_PHOTO_CODE) {
                var yourImage: Bitmap? = null
                try {
                    Thread {
                        val selectedImageUri = data?.data
                        yourImage = if (selectedImageUri == null) {
                            val extra2 = data?.extras
                            extra2?.getParcelable("data")
                        } else {
                            UserPicture(selectedImageUri, contentResolver).bitmap
                        }
                        yourImage?.let {
                            val dir = File(Environment.getExternalStorageDirectory(), "MyFolder/Images")

                            var doSave = true
                            if (!dir.exists()) {
                                doSave = dir.mkdirs()
                            }
                            if (doSave) {
                                var b = saveBitmapToFile(dir, "" + System.currentTimeMillis() + ".jpg"
                                        , yourImage!!, Bitmap.CompressFormat.PNG, 50)
                                val fileSizeInBytes = b?.length()
                                val fileSizeInKB = fileSizeInBytes?.div(1024)
                                val fileSizeInMB = fileSizeInKB?.div(1024)

                                if (b != null) {
                                    if (fileSizeInMB != null) {
                                        if (fileSizeInMB > 1) {
                                            val s = MultiPart.compressImage(Uri.fromFile(b).toString(), this)
                                            b = File(s)
                                        }
                                    }

                                    val filePath = b?.path
                                    val bitmap = BitmapFactory.decodeFile(filePath)
                                    val item = CheckOutRequest()
                                    item.bitmap = bitmap
                                    item.path = filePath
                                    imageList.add(item)
                                }
                            }
                        }
                        runOnUiThread {
                            upload_layout.visibility = View.VISIBLE
                            list_uploaded_image.setItemViewCacheSize(imageList.size.minus(1))
                            (list_uploaded_image.adapter as ImageAdapter).refreshList(imageList)
                        }
                    }.start()

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun getAbsoluteFilePath(data: Intent, activity: Activity): String {
        val uri = data.data
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        val cursor = activity.contentResolver.query(uri!!, projection, null, null, null)
        cursor!!.moveToFirst()
        val columnIndex = cursor.getColumnIndex(projection[0])
        val picturePath = cursor.getString(columnIndex)
        cursor.close()
        return picturePath
    }

    /*Option to the user to select image */
    private fun choosePhoto() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Add Photo!")
        builder.setItems(check) { dialog, item ->
            when {
                check[item] == "Take Photo" -> {
                    openCamera()
                    dialog.dismiss()
                }
                check[item] == "Choose from Gallery" -> {
                    openGalley()
                    dialog.dismiss()
                }
                check[item] == "Cancel" -> dialog.dismiss()
            }
        }
        builder.show()
    }

    /*Open Gallery*/
    private fun openGalley() {
        val i = Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(i, SELECT_IMAGE)
    }

    /* open camera with custome path*/
    private fun openCamera() {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(cameraIntent, TAKE_PHOTO_CODE)
    }

    private fun sendInvitation(finalList: MutableList<UserContactInfo?>) {
        sendInvite(finalList)
    }


    @SuppressLint("SetTextI18n")
    private fun setTotalCount(str: String?) {
        mTotalCommentCount = str;
        mTxtTotalComment?.text = "(" + str + " Comments)"
    }

    private fun isGoogleMapsInstalled(): Boolean {
        var ischeck: Boolean
        try {
            val info: ApplicationInfo = activity.getPackageManager().getApplicationInfo("com.google.android.apps.maps", 0)
            val ischeck1 = info.enabled
            if (ischeck1) {
                ischeck = true
            } else {
                ischeck = false
            }
        } catch (e: PackageManager.NameNotFoundException) {
            ischeck = false
        }

        return ischeck
    }
}
