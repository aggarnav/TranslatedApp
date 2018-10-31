package com.gsoc.ijosa.liquidgalaxycontroller;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.gsoc.ijosa.liquidgalaxycontroller.beans.TourPOI;
import com.gsoc.ijosa.liquidgalaxycontroller.data.POIsContract;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*This fragment is the responsible to create POIs, Tours and Categories*/
public class CreateItemFragment extends Fragment implements OnMapReadyCallback, GoogleMap.OnMarkerDragListener, LocationListener, GoogleMap.OnMapLongClickListener {

    private static final long MIN_TIME = 400;
    private static final float MIN_DISTANCE = 1000;
    static CreateItemFragment fragment;
    private static View rootView= null;
    private static Map<String, String> spinnerIDsAndShownNames;
    private static ArrayList<TourPOI> tourPOIS;
    private static ViewHolderTour viewHolderTour;
    GoogleMap map;
    private LocationManager locationManager;
    private String creationType;
    private Cursor queryCursor;


    public CreateItemFragment() {
        tourPOIS = new ArrayList<>();
    }

    public static CreateItemFragment newInstance() {
        fragment = new CreateItemFragment();
        return fragment;
    }

    /*    To be able to add one POI inside the Tour POIs List, as it is said inside setTourLayoutSettings method,
         user will select one POI by clicking on one instance of POIsFragment and adding it to the list and
        for this reason is why this method is called by POIsFragment class.*/
    public static void setPOItoTourPOIsList(TourPOI tourPOI) throws Exception {

        String global_interval = viewHolderTour.globalInterval.getText().toString();
        if (isNumeric(global_interval)) {//Frist of all, user must type the global interval time value.
            if (!tourPOIS.contains(tourPOI)) {

                FragmentActivity activity = (FragmentActivity) rootView.getContext();

                if (viewHolderTour.addedPois.getCount() == 0 || Integer.parseInt(global_interval) != TourPOIsAdapter.getGlobalInterval()) {
                    TourPOIsAdapter.setGlobalInterval(Integer.parseInt(global_interval));
                    tourPOI.setOrder(tourPOIS.size());
                }

                tourPOI.setDuration(Integer.parseInt(global_interval));

                tourPOIS.add(tourPOI);


                TourPOIsAdapter.setType("creating");
                TourPOIsAdapter adapter = new TourPOIsAdapter(activity, tourPOIS);

                viewHolderTour.addedPois.setAdapter(adapter);

            } else {
                Toast.makeText(rootView.getContext(), "The POI " + tourPOI.getPoiName() + " already exists inside this Tour.", Toast.LENGTH_LONG).show();
            }
        } else {
            throw new Exception("Please, first type a value for the Global POI Interval field.");
        }
    }

    public static void deleteButtonTreatment(View view, final TourPOI tourPoi) {
        //when one POI of the Tours POIs List is deleted, we also have to remove it from the lists
        //we use to help its functionalities.
        final ImageView delete = (ImageView) view.findViewById(R.id.delete);
        screenSizeTreatment(delete);
        delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tourPOIS.remove(tourPoi);
                FragmentActivity activity = (FragmentActivity) rootView.getContext();
                TourPOIsAdapter.setType("creating");
                TourPOIsAdapter adapter = new TourPOIsAdapter(activity, tourPOIS);
                viewHolderTour.addedPois.setAdapter(adapter);
            }
        });
    }

    private static void screenSizeTreatment(ImageView delete) {
        DisplayMetrics metrics = new DisplayMetrics();
        FragmentActivity act = (FragmentActivity) rootView.getContext();
        act.getWindowManager().getDefaultDisplay().getMetrics(metrics);

        int widthPixels = metrics.widthPixels;
        int heightPixels = metrics.heightPixels;
        float scaleFactor = metrics.density;


        //The size of the diagonal in inches is equal to the square root of the height in inches squared plus the width in inches squared.
        float widthDp = widthPixels / scaleFactor;
        float heightDp = heightPixels / scaleFactor;

        float smallestWidth = Math.min(widthDp, heightDp);

        if (smallestWidth >= 1000) {
            delete.setImageResource(R.drawable.ic_remove_circle_black_36dp);
        }
    }

    private static boolean isNumeric(String str) {
        try {
            Integer.parseInt(str);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MapsInitializer.initialize(getActivity());
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        MenuItem itemSettings = menu.findItem(R.id.action_settings);
        if (itemSettings != null) {
            itemSettings.setVisible(false);
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        Bundle extras = getActivity().getIntent().getExtras();
        rootView = null;

        if(extras!=null){
            this.creationType = extras.getString("CREATION_TYPE");
        }

        //When creation button (the once with the arrow's symbol inside, located in POIsFragment) is
        //clicked, this class looks at extras Bundle to know what kind of item it has to create.
        if (creationType != null && creationType.startsWith("POI")) {
            getActivity().setTitle(getResources().getString(R.string.new_poi));
            //If admin user is creating a POI, first of all layout settings are shown on the screen.
            final ViewHolderPoi viewHolder = setPOILayoutSettings(inflater, container);
            viewHolder.createPOI.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {//When POIs Creation button is clicked
                    createPOI(viewHolder);
                }
            });

            SupportMapFragment fragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
            fragment.getMapAsync(this);
        } else if (creationType != null && creationType.startsWith("TOUR")) {
            getActivity().setTitle(getResources().getString(R.string.new_tour));
            setTourLayoutSettings(inflater, container);
            viewHolderTour.createTOUR.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        int tourID = createTour();

                        AddTourToDatabase addTourToDatabase = new AddTourToDatabase(tourID);
                        addTourToDatabase.execute();

                    }catch (NumberFormatException e){
                        Toast.makeText(getActivity(), "The duration of each POI must be in seconds (numeric type).", Toast.LENGTH_LONG).show();
                    }catch (Exception e){
                        if(e.getMessage() != null) {
                            Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                }
            });
        }
        else{//CATEGORY
            getActivity().setTitle(getResources().getString(R.string.new_category));
            final ViewHolderCategory viewHolder = setCategoryLayoutSettings(inflater, container);
            viewHolder.createCategory.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    createCategory(viewHolder);
                }
            });
        }


        locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, MIN_TIME, MIN_DISTANCE, this); //You can also use LocationManager.GPS_PROVIDER and LocationManager.PASSIVE_PROVIDER

        return rootView;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        map.getUiSettings().setMyLocationButtonEnabled(true);
        map.getUiSettings().setRotateGesturesEnabled(true);
        map.getUiSettings().setZoomControlsEnabled(true);
        map.getUiSettings().setMapToolbarEnabled(true);
        map.setMyLocationEnabled(true);
        map.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        map.setOnMapLongClickListener(this);
        map.setOnMarkerDragListener(this);
    }

    @Override
    public void onMarkerDragStart(Marker marker) {

    }

    @Override
    public void onMarkerDrag(Marker marker) {
        ((EditText) rootView.findViewById(R.id.longitude)).setText(String.valueOf(marker.getPosition().longitude));
        ((EditText) rootView.findViewById(R.id.latitude)).setText(String.valueOf(marker.getPosition().latitude));
    }

    @Override
    public void onMarkerDragEnd(Marker marker) {
        ((EditText) rootView.findViewById(R.id.longitude)).setText(String.valueOf(marker.getPosition().longitude));
        ((EditText) rootView.findViewById(R.id.latitude)).setText(String.valueOf(marker.getPosition().latitude));
    }

    @Override
    public void onLocationChanged(Location location) {
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 10);
        if (map != null) {
            map.animateCamera(cameraUpdate);
        }
        locationManager.removeUpdates(this);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onProviderDisabled(String provider) {
    }

    @Override
    public void onMapLongClick(LatLng latLng) {
        map.clear();

        MarkerOptions marker = new MarkerOptions().icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ROSE));
        marker.position(latLng).draggable(true);
        map.addMarker(marker);

        map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, map.getCameraPosition().zoom));

        ((EditText) rootView.findViewById(R.id.longitude)).setText(String.valueOf(marker.getPosition().longitude));
        ((EditText) rootView.findViewById(R.id.latitude)).setText(String.valueOf(marker.getPosition().latitude));

    }

    /*POIs TREATMENT*/
    private void createPOI(ViewHolderPoi viewHolder){
        try {
            //We get the values that user has typed inside input objects.
            ContentValues contentValues = getContentValuesFromDataFromPOIInputForm(viewHolder);

            POIsContract.POIEntry.createNewPOI(getActivity(), contentValues);

            //After creation, the next view page on screen would be the once corresponding to the
            //admin once.
            Intent intent = new Intent(getActivity(), LGPCAdminActivity.class);
            intent.putExtra("comeFrom", "pois");
            startActivity(intent);

        }catch (NumberFormatException e){
            Toast.makeText(getActivity(), getResources().getString(R.string.poiNumericFields), Toast.LENGTH_LONG).show();
        }
    }

    private ContentValues getContentValuesFromDataFromPOIInputForm(ViewHolderPoi viewHolder){

        int categoryID;

        String visitedPlace = viewHolder.visitedPlaceET.getText().toString();
        String completeName = viewHolder.name.getText().toString();
        float longitude = Float.parseFloat(viewHolder.longitudeET.getText().toString());
        float latitude = Float.parseFloat(viewHolder.latitudeET.getText().toString());
        float altitude = Float.parseFloat(viewHolder.altitudeET.getText().toString());
        float heading = Float.parseFloat(viewHolder.headingET.getText().toString());
        float tilt = Float.parseFloat(viewHolder.tiltET.getText().toString());
        float range = Float.parseFloat(viewHolder.rangeET.getText().toString());
        int hide = getHideValueFromInputForm(viewHolder.switchButtonHide);

        String altitudeMode = viewHolder.spinnerAltitudeMode.getSelectedItem().toString();

        //If, in POIsFragment, admin has clicked Creation Here button, the algorythm takes the
        //category ID of the once shown on screen.
        if(creationType.endsWith("HERE")) {
            categoryID = POISFragment.routeID;
        } else if (creationType.endsWith("HERENEW")) {
            Bundle extras = getActivity().getIntent().getExtras();
            categoryID = Integer.parseInt(extras.getString("CATEGORY_ID"));
        }else{
            //Contrary, the algorythm takes the category name selected and gets its ID.
            String shownName = getShownNameValueFromInputForm(viewHolder.categoryID);
            categoryID = getFatherIDValueFromInputForm(shownName);
        }


        ContentValues contentValues = new ContentValues();

        contentValues.put(POIsContract.POIEntry.COLUMN_COMPLETE_NAME, completeName);
        contentValues.put(POIsContract.POIEntry.COLUMN_VISITED_PLACE_NAME, visitedPlace);
        contentValues.put(POIsContract.POIEntry.COLUMN_LONGITUDE, longitude);
        contentValues.put(POIsContract.POIEntry.COLUMN_LATITUDE, latitude);
        contentValues.put(POIsContract.POIEntry.COLUMN_ALTITUDE, altitude);
        contentValues.put(POIsContract.POIEntry.COLUMN_HEADING, heading);
        contentValues.put(POIsContract.POIEntry.COLUMN_TILT, tilt);
        contentValues.put(POIsContract.POIEntry.COLUMN_RANGE, range);
        contentValues.put(POIsContract.POIEntry.COLUMN_ALTITUDE_MODE, altitudeMode);
        contentValues.put(POIsContract.POIEntry.COLUMN_HIDE, hide);
        contentValues.put(POIsContract.POIEntry.COLUMN_CATEGORY_ID, categoryID);

        return contentValues;
    }

    private ViewHolderPoi setPOILayoutSettings(LayoutInflater inflater, ViewGroup container){

        rootView = inflater.inflate(R.layout.fragment_create_or_update_poi, container, false);
        final ViewHolderPoi viewHolder = new ViewHolderPoi(rootView);
        viewHolder.updatePOI.setVisibility(View.GONE);
        viewHolder.createPOI.setVisibility(View.VISIBLE);


        //If user has clicked on Create Here, obviously, no spinner categories option will be shown.
        if (creationType.endsWith("HERE")) {
            viewHolder.categoryID.setVisibility(View.GONE);

            Cursor categories = POIsContract.CategoryEntry.getCategoriesByName(getActivity(), "EARTH");
            long earthCategorycategoryId;

            if (categories != null && categories.moveToFirst()) {
                //Category Exists, we fetch it
                earthCategorycategoryId = POIsContract.CategoryEntry.getIdByShownName(getActivity(), "EARTH/");

                if (POISFragment.routeID != 0 && earthCategorycategoryId != POISFragment.routeID) {
                    rootView.findViewById(R.id.mapPOILayout).setVisibility(View.GONE);
                } else {
                    rootView.findViewById(R.id.mapPOILayout).setVisibility(View.VISIBLE);
                }
            }
        } else if (creationType.endsWith("HERENEW")) {
            viewHolder.categoryID.setVisibility(View.GONE);

            Bundle extras = getActivity().getIntent().getExtras();
            POISFragment.routeID = Integer.parseInt(extras.getString("CATEGORY_ID"));

            Cursor categories = POIsContract.CategoryEntry.getCategoriesByName(getActivity(), "EARTH");
            long earthCategorycategoryId;

            if (categories != null && categories.moveToFirst()) {
                //Category Exists, we fetch it
                earthCategorycategoryId = POIsContract.CategoryEntry.getIdByShownName(getActivity(), "EARTH/");

                //We check if category belongs to earth in order to display the map
                if (categories.getString(categories.getColumnIndex(POIsContract.CategoryEntry.COLUMN_SHOWN_NAME)).toUpperCase().contains("EARTH")) {
                    rootView.findViewById(R.id.mapPOILayout).setVisibility(View.VISIBLE);
                } else if (POISFragment.routeID != 0 && earthCategorycategoryId != POISFragment.routeID) {
                    rootView.findViewById(R.id.mapPOILayout).setVisibility(View.GONE);
                } else {
                    rootView.findViewById(R.id.mapPOILayout).setVisibility(View.VISIBLE);
                }
            }
        } else {
            fillCategorySpinner(viewHolder.categoryID);
        }
        //On the screen there is a button to cancel the creation and return to the main administration view
        setCancelComeBackBehaviour(viewHolder.cancel);

        return viewHolder;
    }

    /*CATEGORIES TREATMENT*/
    private void createCategory(ViewHolderCategory viewHolder){
        //The same with POIs, but with categories
        ContentValues contentValues = getContentValuesFromDataFromCategoryInputForm(viewHolder);

        try{

            POIsContract.CategoryEntry.createNewCategory(getActivity(), contentValues);

            Intent intent = new Intent(getActivity(), LGPCAdminActivity.class);
            intent.putExtra("comeFrom", "categories");
            startActivity(intent);
        }catch(android.database.SQLException e){
            Toast.makeText(getActivity(), getResources().getString(R.string.categoryExists), Toast.LENGTH_LONG).show();
        }
    }

    private ContentValues getContentValuesFromDataFromCategoryInputForm(ViewHolderCategory viewHolder){
        ContentValues contentValues = new ContentValues();

        String categoryName = viewHolder.categoryName.getText().toString();
        int hideValue = getHideValueFromInputForm(viewHolder.switchButtonHide);
        int fatherID;
        String shownName;

        if(creationType.endsWith("HERE")) {
            fatherID = POISFragment.routeID;
            shownName = POIsContract.CategoryEntry.getShownNameByID(getActivity(), fatherID)
                    + viewHolder.categoryName.getText().toString() + "/";
        } else if (creationType.endsWith("HERENEW")) {
            Bundle extras = getActivity().getIntent().getExtras();
            fatherID = Integer.parseInt(extras.getString("CATEGORY_ID"));
            shownName = POIsContract.CategoryEntry.getShownNameByID(getActivity(), fatherID)
                    + viewHolder.categoryName.getText().toString() + "/";
        }else{
            shownName = getShownNameValueFromInputForm(viewHolder.fatherID);
            fatherID = getFatherIDValueFromInputForm(shownName);
            shownName = shownName + viewHolder.categoryName.getText().toString() + "/";
        }

        contentValues.put(POIsContract.CategoryEntry.COLUMN_NAME, categoryName);
        contentValues.put(POIsContract.CategoryEntry.COLUMN_FATHER_ID, fatherID);
        contentValues.put(POIsContract.CategoryEntry.COLUMN_SHOWN_NAME, shownName);
        contentValues.put(POIsContract.CategoryEntry.COLUMN_HIDE, hideValue);

        return contentValues;
    }

    private ViewHolderCategory setCategoryLayoutSettings(LayoutInflater inflater, ViewGroup container) {
        rootView = inflater.inflate(R.layout.fragment_create_or_update_category, container, false);
        final ViewHolderCategory viewHolder = new ViewHolderCategory(rootView);
        viewHolder.updateCategory.setVisibility(View.GONE);
        viewHolder.createCategory.setVisibility(View.VISIBLE);

        if (creationType.endsWith("HERE")) {
            viewHolder.fatherID.setVisibility(View.GONE);
        } else if (creationType.endsWith("HERENEW")) {
            viewHolder.fatherID.setVisibility(View.GONE);
        }else {
            fillCategorySpinner(viewHolder.fatherID);
        }
        setCancelComeBackBehaviour(viewHolder.cancel);
        return viewHolder;
    }

    /*TOUR TREATMENT*/
    private void setTourLayoutSettings(LayoutInflater inflater, ViewGroup container) {
        rootView = inflater.inflate(R.layout.fragment_create_or_update_tour, container, false);
        viewHolderTour = new ViewHolderTour(rootView);
        viewHolderTour.updateTOUR.setVisibility(View.GONE);
        viewHolderTour.createTOUR.setVisibility(View.VISIBLE);
        if(creationType.endsWith("HERE")){
            viewHolderTour.categoryID.setVisibility(View.INVISIBLE);
        }else{
            fillCategorySpinner(viewHolderTour.categoryID);
        }
        setCancelComeBackBehaviour(viewHolderTour.cancel);

        //On the screen will be located an instance of POIsFragment, containing the categories and POIs
        //to add inside the tour to be created.
        POISFragment fragment = new POISFragment();
        Bundle args = new Bundle();
        args.putString("createORupdate", "create");
        args.putString("EDITABLE", "ADMIN/TOUR_POIS");
        fragment.setArguments(args);
        getActivity().getSupportFragmentManager().beginTransaction().replace(R.id.fragment_tour_pois, fragment).commit();
    }

    private int createTour() throws Exception {

        ContentValues contentValues = getContentValuesFromDataFromTourInputForm(viewHolderTour);

        Uri insertedUri = POIsContract.TourEntry.createNewTOUR(getActivity(), contentValues);

        return POIsContract.TourEntry.getIdByUri(insertedUri);
    }

    private ContentValues getContentValuesFromDataFromTourInputForm(ViewHolderTour viewHolder) throws Exception {

        int categoryID;

        String name = viewHolder.tourName.getText().toString();
        if(name.equals("")){
            throw new Exception(getResources().getString(R.string.TourNameExisting));
        }

        int hide = getHideValueFromInputForm(viewHolder.switchButtonHide);
        int interval = Integer.parseInt(viewHolder.globalInterval.getText().toString());
        TourPOIsAdapter.setGlobalInterval(interval);

        if(creationType.endsWith("HERE")) {
            categoryID = POISFragment.routeID;
        }else{
            String shownName = getShownNameValueFromInputForm(viewHolder.categoryID);
            categoryID = getFatherIDValueFromInputForm(shownName);
        }

        ContentValues contentValues = new ContentValues();

        contentValues.put(POIsContract.TourEntry.COLUMN_NAME, name);
        contentValues.put(POIsContract.TourEntry.COLUMN_HIDE, hide);
        contentValues.put(POIsContract.TourEntry.COLUMN_CATEGORY_ID, categoryID);
        contentValues.put(POIsContract.TourEntry.COLUMN_INTERVAL, interval);

        return contentValues;
    }



    /*OTHER UTILITIES*/
    private void fillCategorySpinner(Spinner spinner){

        List<String> list = new ArrayList<>();
        list.add(getResources().getString(R.string.noRouteText));
        spinnerIDsAndShownNames = new HashMap<>();

        //We get all the categories IDs and ShownNames
        queryCursor = POIsContract.CategoryEntry.getIDsAndShownNamesOfAllCategories(getActivity());

        while(queryCursor.moveToNext()){
            spinnerIDsAndShownNames.put(queryCursor.getString(1), String.valueOf(queryCursor.getInt(0)));
            list.add(queryCursor.getString(1));
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, list);
        adapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    } //Fill the spinner with all the categories.

    private int getHideValueFromInputForm(Switch switchButton) {
        int hideValue = 1;
        if (switchButton.isChecked()) {
            hideValue = 0;
        }
        return hideValue;
    }

    private String getShownNameValueFromInputForm(Spinner spinner){
        if(spinner.getSelectedItem() == null || (spinner.getSelectedItem().toString()).equals("NO ROUTE")){
            return "";
        }else{
            return spinner.getSelectedItem().toString();
        }
    }

    private int getFatherIDValueFromInputForm(String shownNameSelected){
        if(shownNameSelected.equals("")){
            return 0;
        }else {
            return Integer.parseInt(spinnerIDsAndShownNames.get(shownNameSelected));
        }
    }

    private void setCancelComeBackBehaviour(android.support.design.widget.FloatingActionButton cancel){

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), LGPCAdminActivity.class);
                startActivity(intent);
            }
        });
    }

    //These three ViewHolder classes are a kind of containers which contain all the elements related
    //to the creation of one item. The POIs once contains the elements for creating a POI, the Tour
    //once to be able to create a Tour and the same with the Categories once.
    public static class ViewHolderPoi {

        public EditText name;
        public FloatingActionButton cancel;
        EditText visitedPlaceET;
        EditText longitudeET;
        EditText latitudeET;
        EditText altitudeET;
        EditText headingET;
        EditText tiltET;
        EditText rangeET;
        Spinner categoryID;
        FloatingActionButton createPOI;
        FloatingActionButton updatePOI;
        Spinner spinnerAltitudeMode;
        private Switch switchButtonHide;

        ViewHolderPoi(final View rootView) {

            name = (EditText) rootView.findViewById(R.id.name);
            visitedPlaceET = (EditText) rootView.findViewById(R.id.visited_place);
            longitudeET = (EditText) rootView.findViewById(R.id.longitude);
            latitudeET = (EditText) rootView.findViewById(R.id.latitude);
            altitudeET = (EditText) rootView.findViewById(R.id.altitude);
            headingET = (EditText) rootView.findViewById(R.id.heading);
            tiltET = (EditText) rootView.findViewById(R.id.tilt);
            rangeET = (EditText) rootView.findViewById(R.id.range);
            spinnerAltitudeMode = (Spinner) rootView.findViewById(R.id.spinnerAltitude);

            categoryID = (Spinner) rootView.findViewById(R.id.categoryID_spinner);
            categoryID.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long selectedItemId) {
                    Cursor categories = POIsContract.CategoryEntry.getCategoriesByName(fragment.getActivity(), "EARTH");
                    long earthCategorycategoryId = 0;
                    if (categories != null && categories.moveToFirst()) {
                        //Category Exists, we fetch it
                        earthCategorycategoryId = POIsContract.CategoryEntry.getIdByShownName(fragment.getActivity(), "EARTH/");

                        if (selectedItemId != 0 && earthCategorycategoryId != selectedItemId) {
                            rootView.findViewById(R.id.mapPOILayout).setVisibility(View.GONE);
                        } else {
                            rootView.findViewById(R.id.mapPOILayout).setVisibility(View.VISIBLE);
                        }
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {

                }
            });

            switchButtonHide = (Switch) rootView.findViewById(R.id.switchButtonHide);
            createPOI = (FloatingActionButton) rootView.findViewById(R.id.create_poi);
            updatePOI = (FloatingActionButton) rootView.findViewById(R.id.update_poi);
            cancel = (FloatingActionButton) rootView.findViewById(R.id.cancel_come_back);
        }
    }

    public static class ViewHolderTour {

        public android.support.design.widget.FloatingActionButton cancel;
        EditText tourName;
        Spinner categoryID;
        android.support.design.widget.FloatingActionButton createTOUR;
        android.support.design.widget.FloatingActionButton updateTOUR;
        ListView addedPois;
        EditText globalInterval;
        private Switch switchButtonHide;

        ViewHolderTour(View rootView) {

            tourName = (EditText) rootView.findViewById(R.id.tour_name);
            switchButtonHide = (Switch) rootView.findViewById(R.id.switchButtonHide);
            categoryID = (Spinner) rootView.findViewById(R.id.categoryID_spinner);
            createTOUR = (android.support.design.widget.FloatingActionButton) rootView.findViewById(R.id.create_tour);
            updateTOUR = (android.support.design.widget.FloatingActionButton) rootView.findViewById(R.id.update_tour);
            addedPois = (ListView) rootView.findViewById(R.id.tour_pois_listview);
            cancel = (android.support.design.widget.FloatingActionButton) rootView.findViewById(R.id.cancel_come_back);
            globalInterval = (EditText) rootView.findViewById(R.id.pois_interval);
        }
    }

    public static class ViewHolderCategory {

        public FloatingActionButton cancel;
        EditText categoryName;
        Spinner fatherID;
        FloatingActionButton createCategory;
        FloatingActionButton updateCategory;
        private Switch switchButtonHide;

        ViewHolderCategory(View rootView) {

            categoryName = (EditText) rootView.findViewById(R.id.category_name);
            switchButtonHide = (Switch) rootView.findViewById(R.id.switchButtonHide);
            fatherID = (Spinner) rootView.findViewById(R.id.father_spinner);
            createCategory = (FloatingActionButton) rootView.findViewById(R.id.create_category);
            updateCategory = (FloatingActionButton) rootView.findViewById(R.id.update_category);
            cancel = (FloatingActionButton) rootView.findViewById(R.id.cancel_come_back);
        }

    }

    private class AddTourToDatabase extends AsyncTask<Void, Void, Void> {

        private int tourId;
        private ProgressDialog dialog;

        AddTourToDatabase(int tourId) {
            this.tourId = tourId;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if (dialog == null) {
                dialog = new ProgressDialog(getActivity());
                dialog.setMessage(getResources().getString(R.string.creating_tour));
                dialog.setIndeterminate(false);
                dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                dialog.setCancelable(true);
                dialog.setCanceledOnTouchOutside(false);
                dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        cancel(true);
                    }
                });
                dialog.show();
            }
        }


        @Override
        protected Void doInBackground(Void... voids) {
            addTourPOIsTODataBase(this.tourId);
            return null;
        }

        private void addTourPOIsTODataBase(int tourID) {

            ContentValues contentValues = new ContentValues();
            int sec;
            int i = 1, pois_number = viewHolderTour.addedPois.getCount(), seconds = 0;
            try {
                int global_interval = Integer.parseInt(viewHolderTour.globalInterval.getText().toString());
                //because all the POIs are inside tourPOIsNames list, we add each one
                for (TourPOI tourPOI : tourPOIS) {
                    contentValues.clear();
                    if (i <= pois_number) {
                        //we get the POI interval time value
                        sec = tourPOI.getDuration();

                        if (sec == 0) {
                            seconds = global_interval;
                        } else {
                            try {
                                seconds = sec;
                            } catch (NumberFormatException e) {
                                Toast.makeText(getActivity(), getResources().getString(R.string.durationIntervalNumeric), Toast.LENGTH_LONG).show();
                            }
                        }
                    }
                    contentValues.put(POIsContract.TourPOIsEntry.COLUMN_POI_ID, tourPOI.getPoiID());
                    contentValues.put(POIsContract.TourPOIsEntry.COLUMN_TOUR_ID, tourID);
                    contentValues.put(POIsContract.TourPOIsEntry.COLUMN_POI_ORDER, i);
                    contentValues.put(POIsContract.TourPOIsEntry.COLUMN_POI_DURATION, seconds);
                    i++;
                    POIsContract.TourPOIsEntry.createNewTourPOI(getActivity(), contentValues);
                }

            } catch (NumberFormatException e) {
                Toast.makeText(getActivity(), getResources().getString(R.string.typeInterval), Toast.LENGTH_LONG).show();
            }

            Intent intent = new Intent(getActivity(), LGPCAdminActivity.class);
            intent.putExtra("comeFrom", "tours");
            startActivity(intent);
        }
    }
}