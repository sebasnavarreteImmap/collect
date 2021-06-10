package org.odk.collect.onic.activities;

import android.content.Intent;
//import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import org.odk.collect.onic.R;
import org.odk.collect.onic.application.Collect;

public class TermsAndConditionsActivity extends AppCompatActivity {

    //new button. CreadoJorge
    private Button to_backButton;

    private Button accept_termnsButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.terms_and_conditions);

        //To BACK button. CreadoJorge
        to_backButton = (Button) findViewById(R.id.to_backButton);
        to_backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Collect.getInstance().getActivityLogger()
                        .logAction(this, "fillBlankForm", "click");
                Intent i = new Intent(getApplicationContext(),
                        AcceptTermsAndConditionsActivity.class); // Call to terms and Conditions Activity
                startActivity(i);
            }
        });

        //Accept Termns button. CreadoJorge
        accept_termnsButton = (Button) findViewById(R.id.accept_termns_button);
        accept_termnsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Collect.getInstance().getActivityLogger()
                        .logAction(this, "fillBlankForm", "click");
                Intent i = new Intent(getApplicationContext(),
                        SelectUserTypeActivity.class); // Call to terms and Conditions Activity
                startActivity(i);
            }
        });


    }


}