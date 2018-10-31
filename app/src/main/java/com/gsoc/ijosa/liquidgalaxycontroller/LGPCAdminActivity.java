package com.gsoc.ijosa.liquidgalaxycontroller;

import android.app.AlarmManager;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.SimpleOnPageChangeListener;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBar.Tab;
import android.support.v7.app.ActionBar.TabListener;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.gsoc.ijosa.liquidgalaxycontroller.data.POIsDbHelper;
import com.gsoc.ijosa.liquidgalaxycontroller.data.POIsProvider;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import java.util.Calendar;

public class LGPCAdminActivity extends ActionBarActivity implements TabListener {
    AdminCollectionPagerAdapter mSectionsPagerAdapter;
    ViewPager mViewPager;

    PendingIntent intent;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lgpcadmin);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        if (Build.VERSION.SDK_INT >= 23) {
            actionBar.setBackgroundDrawable(getResources().getDrawable(R.drawable.button_action_bar));
        }


        this.mSectionsPagerAdapter = new AdminCollectionPagerAdapter(getSupportFragmentManager());
        this.mViewPager = (ViewPager) findViewById(R.id.pager_admin);
        this.mViewPager.setAdapter(this.mSectionsPagerAdapter);
        this.mViewPager.setOnPageChangeListener(new C02741(actionBar));


        for (int i = 0; i < this.mSectionsPagerAdapter.getCount(); i++) {
            actionBar.addTab(actionBar.newTab().setText(this.mSectionsPagerAdapter.getPageTitle(i)).setTabListener(this));
        }

        intent = PendingIntent.getActivity(getBaseContext(), 0, new Intent(getIntent()), PendingIntent.FLAG_ONE_SHOT);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String value = extras.getString("comeFrom");
            if (value != null && value.equalsIgnoreCase("tours")) {
                mViewPager.setCurrentItem(AdminCollectionPagerAdapter.PAGE_TOURS);
            } else if (value != null && value.equalsIgnoreCase("treeView")) {
                mViewPager.setCurrentItem(AdminCollectionPagerAdapter.PAGE_TREEEVIEW);
            }
        }

    }

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_lgpcadmin, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        } else if (id == R.id.reset_db) {
            final AlertDialog.Builder alert = new AlertDialog.Builder(this);
            alert.setTitle(getResources().getString(R.string.are_you_sure_delete_database));

            alert.setPositiveButton(getResources().getString(R.string.yes), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    resetDatabase();
                }
            });

            alert.setNegativeButton(getResources().getString(R.string.no),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                        }
                    });
            alert.show();


            return true;
        } else if (id == R.id.export_db) {
            exportDatabase();
            return true;
        } else if (id == R.id.action_information_help) {
            startActivity(new Intent(this, Help.class));
            return true;
        } else if (id == R.id.action_about) {
            showAboutDialog();
            return true;
        } else if (id != R.id.log_out) {
            return super.onOptionsItemSelected(item);
        } else {
            startActivity(new Intent(this, LGPC.class));
            return true;
        }
    }

    private void showAboutDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.about_dialog);
        dialog.setTitle(getResources().getString(R.string.about_Controller_message));

        Button dialogButton = (Button) dialog.findViewById(R.id.aboutDialogButtonOK);
        dialogButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        dialog.show();
    }

    private void exportDatabase() {
        Log.i("INFO", "EXPORTING DATABASE");
        try {
            File sd = Environment.getExternalStorageDirectory();
            File data = Environment.getDataDirectory();
            if (sd.canWrite()) {
                Calendar c = Calendar.getInstance();
                String dayAndMonth = c.get(Calendar.DAY_OF_MONTH) + "_" + (c.get(Calendar.MONTH) + 1) + "_" + c.get(Calendar.HOUR) + ":" + c.get(Calendar.MINUTE);

                String currentDBPath = "/data/" + this.getPackageName() + "/databases/" + POIsDbHelper.DATABASE_NAME;
                String backupDBPath = "DB_" + dayAndMonth + ".sqlite";


                File currentDB = new File(data, currentDBPath);
                File backupDB = new File(sd, backupDBPath);
                if (currentDB.exists()) {
                    FileChannel src = new FileInputStream(currentDB).getChannel();
                    FileChannel dst = new FileOutputStream(backupDB).getChannel();
                    Log.i("INFO", backupDB.getAbsolutePath());
                    dst.transferFrom(src, 0, src.size());
                    src.close();
                    dst.close();
                }
            }
            Log.i("INFO", "DATABASE EXPORTED");
        } catch (Exception e) {
            Log.e("ERROR", "EXPORTING DATABASE ERROR" + e.getCause());

        }
    }

    public void resetDatabase() {
        ContentResolver resolver = getApplicationContext().getContentResolver();
        ContentProviderClient client = resolver.acquireContentProviderClient(this.getPackageName());
        POIsProvider provider = (POIsProvider) client.getLocalContentProvider();
        provider.resetDatabase();
        client.release();
        resetApp();
    }

    public void resetApp() {

        AlarmManager alm = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
        alm.set(AlarmManager.RTC, System.currentTimeMillis() + 1000, PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), 0));
        System.exit(0);
    }

    public void onTabSelected(Tab tab, FragmentTransaction fragmentTransaction) {
        this.mViewPager.setCurrentItem(tab.getPosition());
    }

    public void onTabUnselected(Tab tab, FragmentTransaction fragmentTransaction) {
    }

    public void onTabReselected(Tab tab, FragmentTransaction fragmentTransaction) {
    }

    /* renamed from: com.gsoc.ijosa.liquidgalaxycontroller.LGPCAdminActivity.1 */
    class C02741 extends SimpleOnPageChangeListener {
        final /* synthetic */ ActionBar val$actionBar;

        C02741(ActionBar actionBar) {
            this.val$actionBar = actionBar;
        }

        public void onPageSelected(int position) {
            this.val$actionBar.setSelectedNavigationItem(position);
        }
    }
}
