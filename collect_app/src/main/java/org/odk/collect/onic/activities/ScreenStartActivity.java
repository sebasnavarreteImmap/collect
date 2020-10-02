package org.odk.collect.onic.activities;

import android.content.Intent;
//import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import org.odk.collect.onic.R;
import org.odk.collect.onic.application.Collect;

public class ScreenStartActivity extends AppCompatActivity {

    private Button getIntoButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.screen_start);

        // start button.
        getIntoButton = (Button) findViewById(R.id.get_intoButton);
        getIntoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Collect.getInstance().getActivityLogger()
                        .logAction(this, "fillBlankForm", "click");
                Intent i = new Intent(getApplicationContext(),
                        AcceptTermsAndConditionsActivity.class);
                startActivity(i);
            }
        });
    }
}