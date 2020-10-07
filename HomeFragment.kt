package com.beerbuddy.views.fragment

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AppCompatActivity
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.widget.AdapterView
import androidx.annotation.RequiresApi
import com.beerbuddy.R
import com.beerbuddy.api.ApiProvider
import com.beerbuddy.utility.Constant
import com.beerbuddy.utility.Constant.BAR_DETAIL
import com.beerbuddy.utility.Constant.LOCATION_PERMISSION
import com.beerbuddy.utility.Constant.REQUEST_CODE_AUTOCOMPLETE
import com.beerbuddy.utility.Constant.googleAPi
import com.beerbuddy.utility.GooglePlacesAutocompleteAdapter
import com.beerbuddy.utility.Session
import com.beerbuddy.views.GlideApp
import com.beerbuddy.views.activity.*
import com.beerbuddy.views.adapter.CheckedInCatAdapter
import com.beerbuddy.views.adapter.DashboardImageAdapter
import com.beerbuddy.views.enum_p.ApiResponseCode
import com.beerbuddy.views.model.*
import com.google.android.gms.location.places.ui.PlaceAutocomplete
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import com.stocktakeonline.custom_dialog.Dialogs_Custom
import com.utils.ConnectiveUtils
import com.utils.DialogBox
import com.utils.GPSTraking
import com.utils.Utility
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_home.*
import kotlinx.android.synthetic.main.other_cotegory_layout.*
import listners.Click
import android.location.LocationManager as LocationManager

class HomeFragment : HomeBaseFragment(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener, GoogleMap.OnMapClickListener {

    private lateinit var activity1: Activity

    var service1: LocationManager? = null
    private var mMap: GoogleMap? = null
    private var isCameraMove: LatLng? = null
    private var mContext: Context? = null
    private var dashboard: DashboardImageAdapter? = null
    private var isFirstLoad = true
    private var search = false
    private var isCurrent = true
    private var markerClick = false
    private var mainGooglePlaceList: GooglePlaceApi? = null
    private var distance = 5.0

    private var textString = "Brewery"
    private var mLocationMain: LatLng? = null;

    private var singleCallMethod: Boolean? = false

    private var first_time_call_api_not_call: Boolean? = false

    private var isCategroyClicked: Boolean? = false
    private val handler = Handler()


    override fun onAttach(context: Context?) {
        super.onAttach(context)
        this.mContext = context
        activity1 = context as Activity
    }

    override fun onMapClick(p0: LatLng?) {
        if (gallery.isShown) {
            val rightSwipe = AnimationUtils.loadAnimation(activity as Context?, R.anim.right_animation)
            gallery.startAnimation(rightSwipe)
            gallery.visibility = View.GONE
            Handler().postDelayed({
                mug_icon.visibility = View.VISIBLE
            }, 1000)
        }
    }

    companion object {
        fun newInstance(): HomeFragment {
            return HomeFragment()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)

        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        if (view != null) {
            (activity as MainActivity).requestPermission(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION)
        }
    }

    fun onPermissionGranted(requestCode: Int) {
        if (requestCode == LOCATION_PERMISSION)
            initialize()
    }

    @SuppressLint("ResourceType")
    private fun initialize() {
        try {
            mug_icon.visibility = View.INVISIBLE
            (childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment).getMapAsync(this)

            // check in buddies refrash in every 3
            val runnableCode = object : Runnable {
                override fun run() {
                    apiCallingCheckInBuddies(true)
                    handler.postDelayed(this,  60 * 1000)
                }
            }

            handler.post(runnableCode)

        } catch (e: Exception) {
            e.printStackTrace()
        }
        onClickMethod()
    }

    //*  when click on search open google activity to search the place*/
    private fun openAutocompleteActivity() {
        activity?.let {
            activity!!.startActivityForResult(Intent(activity, SearchActivity::class.java), Constant.SEARCH_DETAIL)
        }
    }

    //*  Open More View*/
    private fun openMorePageActivity() {

        activity?.let {
            activity!!.startActivityForResult(Intent(activity, MorePageActivity::class.java), Constant.MORE_PAGE)
        }
    }


    @RequiresApi(Build.VERSION_CODES.M)
    @SuppressLint("MissingPermission", "ResourceType")
    override fun onMapReady(googleMap: GoogleMap?) {

        if (!ConnectiveUtils.isNetworkAvailable(mContext)) {
            DialogBox(mContext).showMessage(getString(R.string.network_connection_error), object : DialogBox.OnClickListener {
                override fun onNegativeClick() {
                }

                override fun onPositiveClick() {
                    // activity?.finish()
                }
            })
            return
        }

        mMap = googleMap
        mMap?.uiSettings?.setAllGesturesEnabled(true)
        // We will provide our own zoom controls.
        mMap?.uiSettings?.isZoomControlsEnabled = false

        mMap?.uiSettings?.isMyLocationButtonEnabled = false
        mMap?.uiSettings?.isZoomGesturesEnabled = true
        mMap?.uiSettings?.isRotateGesturesEnabled = false

        mMap?.setMinZoomPreference(6.0f)
        mMap?.setMaxZoomPreference(14.0f)

        try {
            mMap?.isMyLocationEnabled = true
        } catch (e: Exception) {
        }
        GPSTraking().getLastLocation(activity as AppCompatActivity, object : GPSTraking.GetLocation {
            override fun getLocation(Lat: Double, Lon: Double) {
                val myLocation = LatLng(Lat, Lon)
                mLocationMain = myLocation

                isCameraMove = myLocation

                if (isFirstLoad) {
                    val update = CameraUpdateFactory.newLatLngZoom(myLocation, 10f)
                    mMap?.animateCamera(update)
                }

            }
        })

        mMap?.setOnMarkerClickListener(this)

        mMap?.setOnMapClickListener(this)

        current_location_icon?.setOnClickListener {

            GPSTraking().getLastLocation(activity as AppCompatActivity, object : GPSTraking.GetLocation {

                override fun getLocation(Lat: Double, Lon: Double) {
                    hideCategory()
                    clearMap()
                    isCurrent = true
                    val myLocation = LatLng(Lat, Lon)
                    mLocationMain = myLocation
                    val update = CameraUpdateFactory.newLatLngZoom(myLocation, 10f)
                    mMap?.animateCamera(update)
                    activity?.runOnUiThread {
                        txt_search_name.text = getString(R.string.hint_search_location)
                    }
                    current_location_icon.setImageDrawable(activity?.let { it1 -> ContextCompat.getDrawable(it1, R.drawable.gps_location_active_icon) })
                }
            })
        }
        // on camera move listner call the google api
        mMap?.setOnCameraIdleListener {
            if (singleCallMethod == false) {

                (activity as BaseActivity).hideProgressDialog()
                mug_icon.visibility = View.VISIBLE

                Log.e("Home", "CameraIdle")
                val latLng = mMap!!.cameraPosition.target
                if (latLng != null && !isCurrent) {
                    Thread {
                        if (search) {
                            search = false

                        } else {
                            val address = Utility.getAddress(activity, latLng.latitude, latLng.longitude)
                            setAddress(address)
                        }
                    }.start()
                    current_location_icon.setImageDrawable(activity?.let { it1 -> ContextCompat.getDrawable(it1, R.drawable.gps_location_icon) })
                } else {
                    isCurrent = false

                }
                if (latLng != null && latLng.latitude != 0.0 && latLng.longitude != 0.0 && first_time_call_api_not_call == true) {

                    setLocation(latLng)
                } else
                    first_time_call_api_not_call = true
            } else {
                singleCallMethod = false
            }
        }

        mMap?.setOnCameraMoveListener {
            if (gallery.isShown) {
                val rightSwipe = AnimationUtils.loadAnimation(activity, R.anim.right_animation)
                gallery.startAnimation(rightSwipe)
                gallery.visibility = View.GONE
            }
        }

        mMap?.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                        activity, R.raw.text))

        card_search_name.setOnClickListener {
            if (checkPermission()) {
                openAutocompleteActivity()
            } else {
                Dialogs_Custom.singleButtonDialog(activity1, activity1.resources.getString(R.string.app_name_alert),
                        activity1.resources.getString(R.string.alert_location_permission),
                        activity1.resources.getString(R.string.txt_Ok), object : Click {
                    override fun onclick(position: Int, `object`: Any, text: String) {
                        if (text.equals(resources.getString(R.string.ok), true)) {
                        } else {
                        }
                    }
                })
            }
        }

        dashboard = DashboardImageAdapter(mContext!!, null, null)
        gallery.adapter = dashboard

        user_img.setOnClickListener {
            activity?.let {
                val intent = Intent(activity, ProfileActivity::class.java)
                startActivity(intent)
            }
        }

        btn_favorites.setOnClickListener {
            activity?.let {
                activity!!.startActivityForResult(Intent(activity, FavoritesActivity::class.java), BAR_DETAIL)
            }
        }

        mug_icon.setOnClickListener {
            hideCategory()
            apiCallingCheckInBuddies(false)
        }
        gallery.setOnItemClickListener { parent, view, position, id ->
            val bundle = Bundle()
            val beershopdetail = (gallery.adapter as DashboardImageAdapter).getItem(position)
            var result: Result? = null
            val setIt: Iterator<Result?>? = this.mainGooglePlaceList?.results?.asIterable()?.iterator()
            setIt?.let {
                while (setIt.hasNext()) {
                    val e = setIt.next() as Result
                    if (e.id?.equals(beershopdetail?.beer_shop_google_id?.trim()) == true) {
                        print("${e.name} ")
                        result = e
                        break
                    }
                }
            }
            bundle.putParcelable("contact", beershopdetail)
            bundle.putBoolean("isMap", true)
            bundle.putParcelable("data", result)
            activity!!.startActivityForResult(Intent(activity, BarDetailActivity::class.java).putExtras(bundle), BAR_DETAIL)
            val rightSwipe = AnimationUtils.loadAnimation(activity as Context?, R.anim.right_animation)
            gallery.startAnimation(rightSwipe)
            gallery.visibility = View.GONE
            Handler().postDelayed({
                mug_icon.visibility = View.VISIBLE
            }, 1000)
        }
    }


    private fun setAddress(address: String) {
        activity?.runOnUiThread {
            txt_search_name.text = address
        }
    }

    // clear map marker and local data
    private fun clearMap() {
        this@HomeFragment.mainGooglePlaceList?.results?.clear()
        this@HomeFragment.mainGooglePlaceList = null
        mMap?.clear()
        isFirstLoad = true
    }

    // calling of the google api
    @RequiresApi(Build.VERSION_CODES.M)
    @SuppressLint("CheckResult")
    private fun setLocation(current: LatLng) {
        try {
            distance = Utility.distance(current.latitude, current.longitude, isCameraMove?.latitude!!, isCameraMove?.longitude!!)
            if (distance > 20 || isFirstLoad) {
                isFirstLoad = false

                if (current.longitude != 0.0 && current.latitude != 0.0) {
                    // category wise bring the data
                    if (isCategroyClicked == true) {
                        isCategroyClicked = false
                        isCameraMove = current

                        (activity as BaseActivity).showProgressDialog()
                        if (com.beerbuddy.utility.Utility.isOnline(activity!!)) {
                            ApiProvider.findBearShopesBuddies(Constant.findBearBuddies, activity!!)
                                    .findBearShopsBuddies(FindBearShopsPost(current.latitude, current.longitude, textString))
                                    .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                                    .subscribe ({

                                        (activity as BaseActivity).hideProgressDialog()

                                        if (ApiResponseCode.success.code == it?.status && it.data?.results?.size as Int > 0) {
                                            setMarker(it.data)
                                        } else {
                                            (activity as BaseActivity).showProgressDialog()
                                            // calll the method if data does not comtaines in our DB then call google api
                                            ApiProvider.provideService(googleAPi, activity!!).category(current.latitude.toString(),
                                                    current.longitude.toString(), textString).subscribeOn(Schedulers.io())
                                                    .observeOn(AndroidSchedulers.mainThread())
                                                    .subscribe({
                                                        //                            (activity as BaseActivity).hideProgressDialog()
                                                        it?.let {

                                                            (activity as BaseActivity).hideProgressDialog()
                                                            if (it.results?.size as Int > 0) {
                                                                /*mMap?.clear()
                                                                           this.mainGooglePlaceList?.results?.clear()*/
                                                                // save the database to our DB as well to avoid the offen call the google api
                                                                ApiProvider.saveBearShopesBuddies(Constant.saveBearBuddies, activity!!).saveBearShopsBuddies(SaveBeerBuddiesInDB(textString, Location_SaveBeer(current.latitude, current.longitude), it))
                                                                        .subscribeOn(Schedulers.io())
                                                                        .observeOn(AndroidSchedulers.mainThread()).subscribe {
                                                                            Log.d("save data", "sucess")
                                                                        }

                                                                setMarker(it)

                                                            } else {
                                                                if (this.mainGooglePlaceList?.results == null || this.mainGooglePlaceList?.results?.size as Int > 0) {
                                                                    clearMap()
                                                                }
                                                            }
                                                        }
                                                    }, {
                                                        try {
                                                            (activity as BaseActivity).errorHandler(it, activity)
                                                            (activity as BaseActivity).hideProgressDialog()
                                                            if (activity != null) {
                                                                (activity as MainActivity).errorHandler(it, activity as MainActivity)
                                                                clearMap()
                                                            }
                                                        } catch (e: Exception) {

                                                        }
                                                    })
                                        }
                                    }, {
                                        (activity as BaseActivity).errorHandler(it, activity)
                                        (activity as BaseActivity).hideProgressDialog()
                                    })
                        } else {
                            (activity as BaseActivity).hideProgressDialog()
                            Dialogs_Custom.singleButtonDialog(activity1, activity1.resources.getString(R.string.app_name_alert),
                                    activity1.resources.getString(R.string.alert_internet),
                                    activity1.resources.getString(R.string.txt_Ok), object : Click {
                                override fun onclick(position: Int, `object`: Any, text: String) {
                                    if (text.equals(resources.getString(R.string.ok), true)) {
                                    } else {
                                    }
                                }
                            })
                        }
                    }
                } else {
                    (activity as BaseActivity).hideProgressDialog()
                    Dialogs_Custom.singleButtonDialog(activity1, activity1.resources.getString(R.string.app_name_alert),
                            activity1.resources.getString(R.string.location_not_got),
                            activity1.resources.getString(R.string.txt_Ok), object : Click {
                        override fun onclick(position: Int, `object`: Any, text: String) {
                            if (text.equals(resources.getString(R.string.ok), true)) {
                            } else {
                            }
                        }
                    })
                }
            }
        } catch (e: Exception) {
            (activity as BaseActivity).hideProgressDialog()
            e.printStackTrace()
        }
    }

    // method to check if page token key if available than again hit the api
    var count: Int = 0;

    private fun setMarker(list: GooglePlaceApi?) {
        //  Log.d("CALLHOW MANY TIME SET",""+ ++count);
        if (this.mainGooglePlaceList == null) {
            this.mainGooglePlaceList = list
        } else {
            list?.results?.let {
                this.mainGooglePlaceList?.results?.addAll(it)
            }
            this.mainGooglePlaceList?.next_page_token = list?.next_page_token
        }
        if (list?.results?.size as Int > 0) {
            Log.e("Total ->", list.results?.size?.toString())
            setLatLongBound(list)
        }
    }

    // setting the marker on the google map
    @Synchronized
    private fun setBarMarker(list: GooglePlaceApi?, i: Int) {
        list?.let {
            val latLng = LatLng(list.results?.elementAt(i)?.geometry?.location?.lat ?: 0.0,
                    list.results?.elementAt(i)?.geometry?.location?.lng ?: 0.0)
            val name = list.results?.elementAt(i)?.name
            Log.e("name ->", "" + this.tag + " -- " + name)
            setDefaultMarker(name, latLng)
        }
    }

    private fun setLatLongBound(list: GooglePlaceApi?) {

        val builder: LatLngBounds.Builder = LatLngBounds.Builder();
        val lMarkerOptionsList: MutableList<MarkerOptions>? = mutableListOf()
        for (i in 0 until (list?.results?.size ?: 0)) {
            list?.let {
                val latLng = LatLng(list.results?.elementAt(i)?.geometry?.location?.lat
                        ?: 0.0, list.results?.elementAt(i)?.geometry?.location?.lng ?: 0.0)
                builder.include(latLng);
                val name = list.results?.elementAt(i)?.name
                val bitmapDescriptor = BitmapDescriptorFactory.fromBitmap(
                        name?.let { it1 ->
                            com.beerbuddy.utility.Utility.createCustomMarker(R.drawable.img_location_icon, it1)
                        })

                var mMarkerOptions: MarkerOptions? = MarkerOptions().position(latLng).icon(bitmapDescriptor)
                if (mMarkerOptions != null) {
                    mMarkerOptions.title(name)
                    lMarkerOptionsList?.add(mMarkerOptions)
                }
                            }
        }

        if (lMarkerOptionsList != null && lMarkerOptionsList.size as Int > 0) {
            val bounds: LatLngBounds = builder.build();
            lMarkerOptionsList.forEach {
                mMap?.addMarker(it)?.tag = it.title
            }

        }

    }

    @Synchronized
    private fun setDefaultMarker(name: String?, latLng: LatLng) {
        val bitmapDescriptor = BitmapDescriptorFactory.fromBitmap(
                name?.let { it1 ->
                    com.beerbuddy.utility.Utility.createCustomMarker(R.drawable.img_location_icon, it1)
                })
        mMap?.addMarker(MarkerOptions().position(latLng).icon(bitmapDescriptor))?.tag = name;
    }

    // when click on the maker
    @SuppressLint("InflateParams")
    override fun onMarkerClick(p0: Marker?): Boolean {
        if (!markerClick)
            p0?.let {
                markerClick = true
                //piyush
                val item = this.mainGooglePlaceList?.results
                item?.let {
                    makerClickEvent(item, p0)
                }
            }
        return true
    }

    // after clicking find the data from the list
    @Synchronized
    private fun makerClickEvent(item: MutableSet<Result?>?, p0: Marker?) {
        var result: Result? = null
        val setIt: Iterator<Result?>? = item?.asIterable()?.iterator()
        setIt?.let {
            while (setIt.hasNext()) {
                val e = setIt.next() as Result
                if (p0?.tag == e.name) {
                    print("name -- ${e.name} ")
                    result = e
                    break
                }

            }
        }
        val list: MutableSet<Result?>? = mutableSetOf()
        list?.add(result)
        sendDataToServer(list, false)
        markerClick = false
    }


    @SuppressLint("SetTextI18n")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_AUTOCOMPLETE) {
            if (resultCode == Activity.RESULT_OK) {

                val place = PlaceAutocomplete.getPlace(mContext, data)
                if (isCameraMove == null || place.latLng != isCameraMove) {
                    clearMap()
                    search = true
                    place.address?.toString()?.let { it1 -> setAddress(it1) }
                    mLocationMain = place.latLng;
                    val update = CameraUpdateFactory.newLatLngZoom(place.latLng, 10f)
                    mMap?.animateCamera(update)

                }
            }
        }
        if (requestCode == Constant.SEARCH_DETAIL) {
            if (resultCode == Activity.RESULT_OK) {
                if (data != null)
                    data?.let {
                        if (data.getBooleanExtra("is", false)) {
                            //intent.putExtra("no_friend", data.getIntExtra("no_friend", 0))
                            val mAddress = data.getStringExtra("address")

                            val mLatLng = GooglePlacesAutocompleteAdapter.getLocationFromAddress(activity, mAddress)
                            if (mLatLng != null) {
                                if (isCameraMove == null || mLatLng != isCameraMove) {
                                    clearMap()
                                    search = true
                                    setAddress(mAddress)
                                    mLocationMain = mLatLng
                                    val update = CameraUpdateFactory.newLatLngZoom(mLatLng, 10f)
                                    mMap?.animateCamera(update)
                                }
                            }
                        } else {
                            if (data.getBooleanExtra("single", false)) {
                                val mGooglePlaceApi = data?.getParcelableExtra<GooglePlaceApi>("GooglePlaceApi")

                                if (mGooglePlaceApi != null) {
                                    clearMap()
                                    search = true
                                    singleCallMethod = true

                                    if (mGooglePlaceApi.results != null && mGooglePlaceApi.results?.size!! > 0) {
                                        activity?.runOnUiThread {
                                            txt_search_name.text = "" + mGooglePlaceApi.results?.elementAt(0)?.formatted_address
                                        }

                                        val mLatlong = LatLng(mGooglePlaceApi.results?.elementAt(0)?.geometry?.location?.lat!!, mGooglePlaceApi?.results?.elementAt(0)?.geometry?.location?.lng!!)


                                        val update = CameraUpdateFactory.newLatLngZoom(mLatlong, 10f)
                                        mMap?.animateCamera(update)

                                        // if you come for searching then call the method
                                        setMarker(mGooglePlaceApi)
                                    }
                                }

                            }
                        }

                    }

            }
        } else if (requestCode == Constant.MORE_PAGE) {
            if (resultCode == Activity.RESULT_OK) {
                if (data != null)
                    data?.let {
                        if (data.getBooleanExtra("is", false)) {
                            textString = data.getStringExtra("data")
                            getCategoryTypeData(textString, 8)
                        }
                    }
            }
        }
    }

    @SuppressLint("CheckResult")
    private fun apiCallingCheckInBuddies(boolRefrash: Boolean) {
        try {
            ApiProvider.checkInBuddies(Constant.checkInBuddies, activity!!)
                    .checkInBuddies().subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread()).subscribe({
                        if (boolRefrash) {
                           var buddy_count = 0
                            for (i in 0 until it.data?.checkin_data?.size!!){
                                buddy_count = buddy_count + it.data?.checkin_data?.get(i)?.checked_in_buddies?.size!!
                            }
                            batch_chkin_buddies_count.text = buddy_count.toString()
                        } else {
                            getCheckedInBuddies(it)
                        }
                    }, {
                        try {
                            (activity as BaseActivity).errorHandler(it, activity as BaseActivity)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    })

        } catch (e: Exception) {
        }
    }

    private fun getCheckedInBuddies(it: CheckedInBuddies) {

        if (ApiResponseCode.success.code == it?.status) {
            if (!TextUtils.isEmpty(Session.getInstance(this.activity!!)?.getProfile()))
                GlideApp.with(this)
                        .load(activity?.let { it1 -> Session.getInstance(it1)?.getProfile() }).placeholder(R.drawable.profile_waste_icon)
                        .error(R.drawable.profile_waste_icon).into(iv_profile_iamge)

            rel_lyt_buddies_chked_in.visibility = View.VISIBLE
            Log.d("chk buddies count---", "" + it.data?.checkin_data?.size)

           // batch_chkin_buddies_count.text = "" + it.data?.checkin_data?.size
            var buddy_count = 0
            for (i in 0 until it.data?.checkin_data?.size!!){
                buddy_count = buddy_count + it.data?.checkin_data?.get(i)?.checked_in_buddies?.size!!
            }
            batch_chkin_buddies_count.text = buddy_count.toString()

            cross_buddies_chkd_in.setOnClickListener(View.OnClickListener {
                rel_lyt_buddies_chked_in.visibility = View.GONE
            })

            list_check_in_buddies.onItemClickListener = AdapterView.OnItemClickListener { adapterView, view, pos, l ->
                val intent = Intent(activity!!, CheckInBuddiesInResturent::class.java)
                intent.putExtra("buddies_obj", it.data?.checkin_data?.get(pos))
                activity!!.startActivity(intent)
            }
            if (it.data?.checkin_data?.size!! > 0) {
                tv_data_notfound.visibility = View.GONE
            } else {
                tv_data_notfound.visibility = View.VISIBLE
            }
            val adapter = CheckedInCatAdapter(activity!!, it.data?.checkin_data)
            list_check_in_buddies.adapter = adapter
        }
    }

    @SuppressLint("CheckResult")
    private fun sendDataToServer(result: MutableSet<Result?>?, b: Boolean) {
        if (result?.size as Int > 0) {
            (activity as BaseActivity).showProgressDialog()
            ApiProvider.dashboard(Constant.dashboard, activity!!)
                    .dashboard(result).subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ t: BarUserContactList? ->
                        (activity as BaseActivity).hideProgressDialog()
                        if (b) {
                            if (ApiResponseCode.success.code == t?.status) {
                                mug_icon.visibility = View.GONE
                                (gallery.adapter as DashboardImageAdapter).refresh(t.data?.beershopdetails, this.mainGooglePlaceList?.results)
                                gallery.setSelection(0)
                                gallery.visibility = View.VISIBLE
                                val rightSwipe = AnimationUtils.loadAnimation(activity, R.anim.left_animation)
                                gallery.startAnimation(rightSwipe)
                            }
                        } else {
                            if (ApiResponseCode.success.code == t?.status) {
                                val bundle = Bundle()
                                bundle.putParcelable("contact", t.data?.beershopdetails?.get(0))
                                bundle.putParcelable("data", result?.elementAt(0))
                                bundle.putBoolean("isIndividual", true)
                                bundle.putBoolean("isMap", true)
                                activity!!.startActivityForResult(Intent(activity, BarDetailActivity::class.java).putExtras(bundle), BAR_DETAIL)
                            }
                        }
                    }
                            , { t: Throwable? ->
                        (activity as BaseActivity).hideProgressDialog()
                        (activity as BaseActivity).errorHandler(t, activity as BaseActivity)
                    })
        }
    }

    override fun onResume() {
        super.onResume()
        if (!TextUtils.isEmpty(Session.getInstance(this.activity!!)?.getProfile()))
            GlideApp.with(this)
                    .load(activity?.let { it1 -> Session.getInstance(it1)?.getProfile() }).placeholder(R.drawable.profile_waste_icon)
                    .error(R.drawable.profile_waste_icon).into(user_img)
    }


    //onclick method
    private fun onClickMethod() {
        onclick()
    }

    private fun onclick() {
        try {
            ll_bevery.setOnClickListener {
                getCategoryTypeData(getString(R.string.brewery), 1)
            }

            ll_pub.setOnClickListener {
                getCategoryTypeData(getString(R.string.txt_pub), 2)
            }

            ll_gastPro.setOnClickListener {
                getCategoryTypeData(getString(R.string.gastropub), 3)
            }

            ll_sports_bar.setOnClickListener {
                getCategoryTypeData(getString(R.string.sports_bar_), 4)
            }

            ll_bar.setOnClickListener {
                getCategoryTypeData(getString(R.string.bar_bar), 5)
            }

            ll_bar_grill.setOnClickListener {
                getCategoryTypeData(getString(R.string.bar_and_grill_), 6)
            }

            ll_resturent.setOnClickListener {
                getCategoryTypeData(getString(R.string.txt_restaurant), 7)
            }

            ll_more.setOnClickListener {
                openMorePageActivity()
            }
        } catch (e: Exception) {
        }
    }

    private fun getCategoryTypeData(textStringLocal: String, selectionType: Int) {

        isCategroyClicked = true
        textString = textStringLocal
        setColor(selectionType)
        if (checkPermission()) {
            if (checkGPS()) {
                clickCategory(textStringLocal)
            } else {
                Dialogs_Custom.singleButtonDialog(activity1, activity1.resources.getString(R.string.app_name_alert),
                        activity1.resources.getString(R.string.alert_gps_serviice),
                        activity1.resources.getString(R.string.txt_Ok), object : Click {
                    override fun onclick(position: Int, `object`: Any, text: String) {
                        if (text.equals(resources.getString(R.string.ok), true)) {
                        } else {
                        }
                    }
                })
            }
        } else {
            Dialogs_Custom.singleButtonDialog(activity1, activity1.resources.getString(R.string.app_name_alert),
                    activity1.resources.getString(R.string.alert_location_permission),
                    activity1.resources.getString(R.string.txt_Ok), object : Click {
                override fun onclick(position: Int, `object`: Any, text: String) {
                    if (text.equals(resources.getString(R.string.ok), true)) {
                    } else {
                    }
                }
            })
        }

    }


    private fun checkGPS(): Boolean {
        val lm = activity!!.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        var enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
        return enabled
    }

    @SuppressLint("CheckResult")
    private fun clickCategory(name_cat: String?) {
        hideCategory()

        if (mLocationMain != null) {

            clearMap()
            isCurrent = true
            val update = CameraUpdateFactory.newLatLngZoom(mLocationMain, 10f)
            mMap?.animateCamera(update)
            activity?.runOnUiThread {
                // txt_search_name.text = getString(R.string.hint_search_location)
            }
            current_location_icon.setImageDrawable(activity?.let { it1 -> ContextCompat.getDrawable(it1, R.drawable.gps_location_active_icon) })
        }
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        if (isVisibleToUser) {
            fragmentManager!!.beginTransaction().detach(this).attach(this).commit()
        }
    }


    private fun showDialog(activity: Activity, messages: String) {

    }

}

