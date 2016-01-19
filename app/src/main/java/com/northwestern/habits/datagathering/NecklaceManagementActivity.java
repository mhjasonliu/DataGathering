package com.northwestern.habits.datagathering;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class NecklaceManagementActivity extends AppCompatActivity {

//    public final String AUDIO_EXTRA = "audio";
//    public final String PIEZO_EXTRA = "piezo";
    public static final String NAME_EXTRA = "name";
    public static final String MAC_EXTRA = "mac";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_necklace_management);

    }
}
